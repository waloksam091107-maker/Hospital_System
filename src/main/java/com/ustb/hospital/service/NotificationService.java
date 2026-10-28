package com.ustb.hospital.service;

import com.ustb.hospital.utils.MybatisUtil;
import org.apache.ibatis.session.SqlSession;
import java.sql.*;
import java.util.*;
import java.util.function.Supplier;

/** Recipient-scoped notifications. Business messages use the caller's transaction. */
public class NotificationService {
    private final Supplier<SqlSession> transactions;
    private final Supplier<SqlSession> reads;

    public NotificationService() { this(MybatisUtil::getTransactionSqlSession, MybatisUtil::getSqlSession); }
    public NotificationService(Supplier<SqlSession> transactions, Supplier<SqlSession> reads) {
        this.transactions=Objects.requireNonNull(transactions); this.reads=Objects.requireNonNull(reads);
    }

    /**
     * Writes on the supplied connection and never commits or rolls back it.
     * Callers MUST use session.commit(true)/rollback(true) if their transaction
     * contains only raw JDBC: MyBatis does not track JDBC writes as dirty.
     */
    public static void enqueue(SqlSession session, String role, int recipientId, String eventKey,
                               String type, String title, String body, String targetType, String targetId) {
        identity(role,recipientId);
        required(eventKey,191); required(type,40); required(title,200); required(body,2000);
        optional(targetType,40); optional(targetId,100);
        Objects.requireNonNull(session,"session");
        String sql="INSERT INTO notifications (recipient_role,recipient_id,event_key,notification_type,title,body,target_type,target_id) VALUES (?,?,?,?,?,?,?,?)";
        try (PreparedStatement statement=session.getConnection().prepareStatement(sql)) {
            statement.setString(1,role); statement.setInt(2,recipientId); statement.setString(3,eventKey);
            statement.setString(4,type); statement.setString(5,title); statement.setString(6,body);
            statement.setString(7,targetType); statement.setString(8,targetId); statement.executeUpdate();
        } catch (SQLException failure) {
            // Do not mask truncation, connectivity, CHECK/FK errors, or other failures.
            if (duplicate(failure) && eventExists(session,role,recipientId,eventKey)) return;
            throw new IllegalStateException("站内通知写入失败。",failure);
        }
    }

    public Map<String,Object> list(String role,int recipientId,int page,int pageSize) {
        identity(role,recipientId);
        if(page<1 || pageSize<1 || pageSize>100) throw new IllegalArgumentException("页码或每页条数无效。");
        try (SqlSession session=reads.get()) {
            Connection connection=session.getConnection(); long total;
            try(PreparedStatement count=connection.prepareStatement("SELECT COUNT(*) FROM notifications WHERE recipient_role=? AND recipient_id=?")) {
                recipient(count,role,recipientId); try(ResultSet rows=count.executeQuery()) { rows.next(); total=rows.getLong(1); }
            }
            List<Map<String,Object>> items=new ArrayList<>();
            try(PreparedStatement statement=connection.prepareStatement("SELECT id,notification_type,title,body,target_type,target_id,created_at,read_at FROM notifications WHERE recipient_role=? AND recipient_id=? ORDER BY created_at DESC,id DESC LIMIT ? OFFSET ?")) {
                recipient(statement,role,recipientId); statement.setInt(3,pageSize); statement.setLong(4,((long)page-1)*pageSize);
                try(ResultSet rows=statement.executeQuery()) {
                    while(rows.next()) {
                        Map<String,Object> item=new LinkedHashMap<>(); item.put("id",rows.getLong("id"));
                        item.put("type",rows.getString("notification_type")); item.put("title",rows.getString("title")); item.put("body",rows.getString("body"));
                        item.put("targetType",rows.getString("target_type")); item.put("targetId",rows.getString("target_id"));
                        item.put("createdAt",rows.getString("created_at")); item.put("readAt",rows.getString("read_at")); items.add(item);
                    }
                }
            }
            Map<String,Object> result=new LinkedHashMap<>(); result.put("success",true); result.put("role",role);
            result.put("items",items); result.put("total",total); result.put("page",page); result.put("pageSize",pageSize); return result;
        } catch(SQLException failure) { throw new IllegalStateException("站内通知读取失败。",failure); }
    }

    public long countUnread(String role,int recipientId) {
        identity(role,recipientId);
        try(SqlSession session=reads.get(); PreparedStatement statement=session.getConnection().prepareStatement("SELECT COUNT(*) FROM notifications WHERE recipient_role=? AND recipient_id=? AND read_at IS NULL")) {
            recipient(statement,role,recipientId); try(ResultSet rows=statement.executeQuery()) { rows.next(); return rows.getLong(1); }
        } catch(SQLException failure) { throw new IllegalStateException("未读通知读取失败。",failure); }
    }

    public boolean markRead(String role,int recipientId,long notificationId) {
        identity(role,recipientId); if(notificationId<=0) throw new IllegalArgumentException("消息编号无效。");
        try(SqlSession session=transactions.get()) {
            try {
                int changed;
                try(PreparedStatement statement=session.getConnection().prepareStatement("UPDATE notifications SET read_at=CURRENT_TIMESTAMP WHERE recipient_role=? AND recipient_id=? AND id=? AND read_at IS NULL")) {
                    recipient(statement,role,recipientId); statement.setLong(3,notificationId); changed=statement.executeUpdate();
                }
                boolean owned=changed==1;
                if(!owned) try(PreparedStatement statement=session.getConnection().prepareStatement("SELECT id FROM notifications WHERE recipient_role=? AND recipient_id=? AND id=?")) {
                    recipient(statement,role,recipientId); statement.setLong(3,notificationId); try(ResultSet rows=statement.executeQuery()) { owned=rows.next(); }
                }
                session.commit(true); return owned;
            } catch(SQLException | RuntimeException failure) { session.rollback(true); throw new IllegalStateException("通知已读操作失败。",failure); }
        }
    }

    public int markAllRead(String role,int recipientId) {
        identity(role,recipientId);
        try(SqlSession session=transactions.get()) {
            try(PreparedStatement statement=session.getConnection().prepareStatement("UPDATE notifications SET read_at=CURRENT_TIMESTAMP WHERE recipient_role=? AND recipient_id=? AND read_at IS NULL")) {
                recipient(statement,role,recipientId); int changed=statement.executeUpdate(); session.commit(true); return changed;
            } catch(SQLException | RuntimeException failure) { session.rollback(true); throw new IllegalStateException("通知已读操作失败。",failure); }
        }
    }

    private static boolean eventExists(SqlSession session,String role,int id,String event) {
        try(PreparedStatement statement=session.getConnection().prepareStatement("SELECT id FROM notifications WHERE recipient_role=? AND recipient_id=? AND event_key=?")) {
            recipient(statement,role,id); statement.setString(3,event); try(ResultSet rows=statement.executeQuery()) { return rows.next(); }
        } catch(SQLException failure) { throw new IllegalStateException("站内通知幂等校验失败。",failure); }
    }
    private static boolean duplicate(SQLException failure) { return failure.getErrorCode()==1062 || "23505".equals(failure.getSQLState()); }
    private static void recipient(PreparedStatement statement,String role,int id) throws SQLException { statement.setString(1,role); statement.setInt(2,id); }
    private static void identity(String role,int id) { if(id<=0 || !("PATIENT".equals(role)||"DOCTOR".equals(role)||"ADMIN".equals(role))) throw new IllegalArgumentException("接收账号无效。"); }
    private static void required(String text,int limit) { if(text==null || text.trim().isEmpty() || text.length()>limit) throw new IllegalArgumentException("通知内容无效。"); }
    private static void optional(String text,int limit) { if(text!=null && text.length()>limit) throw new IllegalArgumentException("通知关联信息无效。"); }
}
