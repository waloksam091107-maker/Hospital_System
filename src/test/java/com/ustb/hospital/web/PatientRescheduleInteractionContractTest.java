package com.ustb.hospital.web;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Guards the patient reschedule control against inert javascript:void links. */
public class PatientRescheduleInteractionContractTest {
    @Test
    public void bookedAppointmentUsesBoundButtonAndTopLevelModal() throws Exception {
        String page = new String(Files.readAllBytes(Paths.get(
                "src", "main", "webapp", "hospro", "patient.jsp")), StandardCharsets.UTF_8);

        assertFalse("改期控件不能依赖内联 onclick + javascript:void 链接", page.contains("onclick='showUpdateModal("));
        assertTrue("已预约记录必须有明确绑定的改期点击事件", page.contains("rescheduleButton.on('click'"));
        assertTrue("改期弹窗必须移到 body 顶层，避免被模板容器裁切", page.contains("$('#updateModal').appendTo('body')"));
    }
}
