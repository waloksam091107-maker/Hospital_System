"""Opt-in end-to-end checks against a disposable locally seeded clinic container.

Requires ignored .hospital-local/five-extensions-http-fixture.json. Refuses business
ports and never discovers production credentials. Does not publish or commit.
"""
import datetime
from decimal import Decimal
import http.cookiejar
import json
import pathlib
import time
import urllib.error
import urllib.parse
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[1]
FIXTURE = json.loads((ROOT / '.hospital-local/five-extensions-http-fixture.json').read_text('utf-8-sig'))
assert FIXTURE['prefix'].startswith('http_') and len(FIXTURE['prefix']) == 15
BASE = 'http://127.0.0.1:18081/ustbhospital'
CHECKS = 0


def check(condition, message):
    global CHECKS
    assert condition, message
    CHECKS += 1


class Client:
    def __init__(self):
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
        self.token = None

    def request(self, path, form=None, expected=200, csrf=True):
        headers = {}
        if form is not None and csrf and self.token:
            headers['X-CSRF-Token'] = self.token
        request = urllib.request.Request(BASE + path, headers=headers,
                                         data=None if form is None else urllib.parse.urlencode(form).encode('utf-8'))
        try:
            response = self.opener.open(request, timeout=15)
        except urllib.error.HTTPError as error:
            response = error
        body = response.read().decode('utf-8')
        check(response.status == expected, f'{path}: expected HTTP {expected}, received {response.status}')
        try:
            return json.loads(body)
        except json.JSONDecodeError:
            return {'url': response.geturl(), 'html': body}

    def login(self, role, username=None, password=None, forced=False):
        result = self.request('/auth/login', {'roleCode': str(role), 'user': username or FIXTURE['prefix'],
                                             'password': password or FIXTURE['password']}, csrf=False)
        check('login_failed' not in result.get('url', ''), 'test account login failed')
        if forced:
            check('required=true' in result.get('url', ''), 'temporary password did not require change')
        self.token = self.request('/security/csrf')['token']


anonymous = Client()
for attempt in range(45):
    try:
        if urllib.request.urlopen(BASE + '/health/ready', timeout=3).status == 200:
            break
    except (OSError, urllib.error.URLError):
        time.sleep(1)
else:
    raise AssertionError('isolated application readiness failed')

anonymous.request('/clinic/queue', expected=401)
anonymous.request('/notifications/list', expected=401)
admin, doctor, patient, other, third, past_doctor = [Client() for _ in range(6)]
admin.login(3)
doctor.login(1)
patient.login(2)
other.login(2, FIXTURE['prefix'] + '-other')
third.login(2, FIXTURE['prefix'] + '-third')
past_doctor.login(1, FIXTURE['prefix'] + '-past')
past_doctor.request('/clinic/slots', {'action': 'capacity', 'slotId': FIXTURE['slot'], 'capacity': 5}, expected=403)
patient.request('/clinic/slots', {'action': 'capacity', 'slotId': FIXTURE['slot'], 'capacity': 2}, expected=403)
doctor.request('/clinic/slots', {'action': 'capacity', 'slotId': FIXTURE['slot'], 'capacity': 2}, expected=403, csrf=False)
doctor.request('/clinic/slots', {'action': 'capacity', 'slotId': FIXTURE['slot'], 'capacity': 2})
for client in (patient, other):
    client.request('/appointments/book', {'doctorId': FIXTURE['doctor'], 'appointment_date': FIXTURE['today'],
                                         'appointment_time': 'EVENING'})
appointments = patient.request('/clinic/queue')['list']
mine = next(item for item in appointments if item['doctorId'] == FIXTURE['doctor'])
appointment_id = mine['appointmentId']
check(mine['appointmentSequence'] in (1, 2), 'appointment sequence missing')
slots = patient.request('/clinic/slots?' + urllib.parse.urlencode({'doctorId': FIXTURE['doctor'], 'date': FIXTURE['today']}))['list']
check(slots[0]['remaining'] == 0, 'full capacity not reflected')
full = third.request('/appointments/book', {'doctorId': FIXTURE['doctor'], 'appointment_date': FIXTURE['today'], 'appointment_time': 'EVENING'}, expected=409)
check(full['code'] == 'SLOT_UNAVAILABLE', 'full capacity did not block a third patient')
check(Decimal(str(third.request('/portal/profile')['balance'])) == Decimal('100'), 'failed booking charged balance')
patient.request('/appointments/book', {'doctorId': FIXTURE['doctor'], 'appointment_date': FIXTURE['today'], 'appointment_time': 'EVENING'}, expected=409)
doctor.request('/clinic/slots', {'action': 'capacity', 'slotId': FIXTURE['slot'], 'capacity': 1}, expected=409)
other.request('/clinic/queue', {'action': 'check-in', 'appointmentId': appointment_id}, expected=403)
doctor.request('/appointments/start', {'appointmentId': appointment_id}, expected=409)
patient.request('/clinic/queue', {'action': 'check-in', 'appointmentId': appointment_id})
doctor.request('/clinic/queue', {'action': 'call-next', 'date': FIXTURE['today'], 'period': 'EVENING'})
doctor.request('/clinic/queue', {'action': 'call-next', 'date': FIXTURE['today'], 'period': 'EVENING'}, expected=409)
doctor.request('/clinic/queue', {'action': 'skip', 'appointmentId': appointment_id})
doctor.request('/clinic/queue', {'action': 'requeue', 'appointmentId': appointment_id})
doctor.request('/clinic/queue', {'action': 'call-next', 'date': FIXTURE['today'], 'period': 'EVENING'})
doctor.request('/appointments/start', {'appointmentId': appointment_id})
doctor.request('/medical-records/finalize', {'appointmentId': appointment_id, 'diagnosis': '隔离测试诊断',
                                          'treatmentPlan': '隔离测试方案', 'medicalAdvice': '复诊观察'})
mine = next(item for item in patient.request('/clinic/queue')['list'] if item['appointmentId'] == appointment_id)
check(mine['status'] == 'COMPLETED' and mine['queueStatus'] == 'DONE', 'clinical and queue completion differ')
messages = patient.request('/notifications/list?page=1&pageSize=100')['items']
check(any(item['type'] == 'MEDICAL_RECORD_FINAL' for item in messages), 'final record notification missing')
other.request('/notifications/read', {'id': messages[0]['id']}, expected=404)
unread_before = patient.request('/notifications/unread-count')['unreadCount']
patient.request('/notifications/read', {'id': messages[0]['id']})
check(patient.request('/notifications/unread-count')['unreadCount'] == unread_before - 1, 'single read count is wrong')
patient.request('/notifications/read', {'id': messages[0]['id']})
check(patient.request('/notifications/unread-count')['unreadCount'] == unread_before - 1, 'repeated read changed count again')
page_one = patient.request('/notifications/list?page=1&pageSize=1')
page_two = patient.request('/notifications/list?page=2&pageSize=1')
check(page_one['items'][0]['id'] != page_two['items'][0]['id'], 'notification pages repeat same message')
patient.request('/notifications/list?page=0', expected=400)
patient.request('/notifications/read-all', {})
check(patient.request('/notifications/unread-count')['unreadCount'] == 0, 'read-all did not clear unread')
original_other = next(item for item in other.request('/clinic/queue')['list'] if item['doctorId'] == FIXTURE['doctor'] and item['appointmentDate'] == FIXTURE['today'])
other.request('/appointments/reschedule', {'appointmentId': original_other['appointmentId'], 'appointmentDate': FIXTURE['tomorrow'], 'appointmentTime': 'EVENING'})
other_items = other.request('/clinic/queue')['list']
check(next(item for item in other_items if item['appointmentId'] == original_other['appointmentId'])['status'] == 'RESCHEDULED', 'original rescheduled appointment lost history')
rescheduled = next(item for item in other_items if item['doctorId'] == FIXTURE['doctor'] and item['appointmentDate'] == FIXTURE['tomorrow'])
check(rescheduled['previousAppointmentId'] == original_other['appointmentId'], 'reschedule link missing')
check(Decimal(str(other.request('/portal/profile')['balance'])) == Decimal('90'), 'reschedule charged twice')
other.request('/clinic/queue', {'action': 'check-in', 'appointmentId': rescheduled['appointmentId']}, expected=409)
third.request('/appointments/book', {'doctorId': FIXTURE['doctor'], 'appointment_date': FIXTURE['today'], 'appointment_time': 'EVENING'})
third_item = next(item for item in third.request('/clinic/queue')['list'] if item['doctorId'] == FIXTURE['doctor'] and item['appointmentDate'] == FIXTURE['today'])
check(third_item['appointmentSequence'] == 3, 'released appointment number was reused')
doctor.request('/clinic/queue', {'action': 'call-next', 'date': FIXTURE['today'], 'period': 'EVENING'}, expected=409)
past_doctor.request('/clinic/queue', {'action': 'no-show', 'appointmentId': FIXTURE['waitingAppointment']}, expected=409)
past_doctor.request('/clinic/queue', {'action': 'no-show', 'appointmentId': FIXTURE['noShowAppointment']})
past_doctor.request('/clinic/queue', {'action': 'no-show', 'appointmentId': FIXTURE['noShowAppointment']}, expected=409)
no_show_item = next(item for item in patient.request('/clinic/queue')['list'] if item['appointmentId'] == FIXTURE['noShowAppointment'])
check(no_show_item['status'] == 'NO_SHOW', 'no-show state was not persisted')
past_doctor.request('/clinic/slots', {'action': 'capacity', 'slotId': FIXTURE['pastSlot'], 'capacity': 1}, expected=400)
extra_day = (datetime.date.fromisoformat(FIXTURE['today']) + datetime.timedelta(days=2)).isoformat()
apply_data = {'action': 'apply', 'startDate': extra_day, 'endDate': extra_day, 'periods': 'AM', 'reason': '隔离测试审批分支'}
invalid = dict(apply_data, periods=',')
doctor.request('/clinic/leave', invalid, expected=400)
invalid['periods'] = 'AM,,'
doctor.request('/clinic/leave', invalid, expected=400)
doctor.request('/clinic/leave', apply_data)
branch = next(item for item in doctor.request('/clinic/leave')['list'] if item['startDate'] == extra_day)
doctor.request('/clinic/leave', apply_data, expected=409)
patient.request('/clinic/leave', {'action': 'approve', 'id': branch['leaveId'], 'reason': '越权'}, expected=403)
admin.request('/clinic/leave', {'action': 'reject', 'id': branch['leaveId'], 'reason': '正常接诊'})
check(next(item for item in doctor.request('/clinic/leave')['list'] if item['leaveId'] == branch['leaveId'])['status'] == 'REJECTED', 'reject state missing')
doctor.request('/clinic/leave', apply_data)
branch = next(item for item in doctor.request('/clinic/leave')['list'] if item['startDate'] == extra_day and item['status'] == 'PENDING')
past_doctor.request('/clinic/leave', {'action': 'withdraw', 'id': branch['leaveId']}, expected=403)
doctor.request('/clinic/leave', {'action': 'withdraw', 'id': branch['leaveId']})
check(next(item for item in doctor.request('/clinic/leave')['list'] if item['leaveId'] == branch['leaveId'])['status'] == 'WITHDRAWN', 'withdraw state missing')
doctor.request('/clinic/leave', apply_data)
branch = next(item for item in doctor.request('/clinic/leave')['list'] if item['startDate'] == extra_day and item['status'] == 'PENDING')
admin.request('/clinic/leave', {'action': 'approve', 'id': branch['leaveId'], 'reason': '同意'})
doctor.request('/schedules/create', {'date': extra_day, 'shang': 1, 'xia': 0, 'wan': 0})
new_sources = doctor.request('/clinic/slots?date=' + extra_day)['list']
new_am = next(item for item in new_sources if item['slotPeriod'] == 'AM')
check(new_am['capacity'] == 10 and new_am['leaveBlocked'] and new_am['remaining'] == 0, 'new schedule bypassed approved leave or lost default capacity')
admin.request('/clinic/leave', {'action': 'revoke', 'id': branch['leaveId'], 'reason': '恢复'})
new_sources = doctor.request('/clinic/slots?date=' + extra_day)['list']
check(next(item for item in new_sources if item['slotPeriod'] == 'AM')['remaining'] == 10, 'revoked leave failed to restore source')
check(next(item for item in new_sources if item['slotPeriod'] == 'PM')['remaining'] == 0, 'revocation opened a manually closed period')

doctor.request('/clinic/leave', {'action': 'apply', 'startDate': FIXTURE['tomorrow'], 'endDate': FIXTURE['tomorrow'],
                               'periods': 'EVENING', 'reason': '隔离测试请假'})
leave = next(item for item in admin.request('/clinic/leave')['list'] if item['doctorId'] == FIXTURE['doctor'])
doctor.request('/clinic/leave', {'action': 'approve', 'id': leave['leaveId'], 'reason': '自己审批'}, expected=403)
admin.request('/clinic/leave', {'action': 'approve', 'id': leave['leaveId'], 'reason': '同意'})
admin.request('/clinic/leave', {'action': 'approve', 'id': leave['leaveId'], 'reason': '重复审批'}, expected=409)
upcoming = next(item for item in other.request('/clinic/queue')['list'] if item['appointmentDate'] == FIXTURE['tomorrow'])
check(upcoming['status'] == 'BOOKED' and upcoming['leaveBlocked'], 'leave did not retain and freeze appointment')
patient.request('/appointments/book', {'doctorId': FIXTURE['doctor'], 'appointment_date': FIXTURE['tomorrow'], 'appointment_time': 'EVENING'}, expected=409)
other.request('/appointments/cancel', {'appointmentId': upcoming['appointmentId']})
admin.request('/clinic/leave', {'action': 'revoke', 'id': leave['leaveId'], 'reason': '恢复接诊'})
admin.request('/portal/admin/account-security/disable', {'role': 'DOCTOR', 'accountId': FIXTURE['doctor']})
doctor.request('/clinic/queue', expected=401)
patient.request('/appointments/book', {'doctorId': FIXTURE['doctor'], 'appointment_date': FIXTURE['tomorrow'], 'appointment_time': 'EVENING'}, expected=409)
admin.request('/portal/admin/account-security/enable', {'role': 'DOCTOR', 'accountId': FIXTURE['doctor']})
doctor.request('/clinic/queue', expected=401)
doctor.login(1)
patient.request('/appointments/book', {'doctorId': FIXTURE['doctor'], 'appointment_date': FIXTURE['tomorrow'], 'appointment_time': 'EVENING'})
restored = next(item for item in patient.request('/clinic/queue')['list'] if item['doctorId'] == FIXTURE['doctor'] and item['appointmentDate'] == FIXTURE['tomorrow'])
check(restored['appointmentSequence'] == 2, 'cancelled future appointment number reused')
for attempt in range(5):
    failed = Client().request('/auth/login', {'roleCode': 1, 'user': FIXTURE['prefix'], 'password': 'incorrect-test-password'}, csrf=False)
    check('login_failed' in failed.get('url', ''), 'wrong password unexpectedly accepted')
locked = Client().request('/auth/login', {'roleCode': 1, 'user': FIXTURE['prefix'], 'password': FIXTURE['password']}, csrf=False)
check('login_failed' in locked.get('url', ''), 'fifth login failure did not lock the account')
admin.request('/portal/admin/account-security/unlock', {'role': 'DOCTOR', 'accountId': FIXTURE['doctor']})
recovered_doctor = Client()
recovered_doctor.login(1)
recovered_doctor.request('/clinic/queue')
admin.request('/portal/admin/account-security/disable', {'role': 'PATIENT', 'accountId': FIXTURE['third']})
third.request('/clinic/queue', expected=401)
admin.request('/portal/admin/account-security/enable', {'role': 'PATIENT', 'accountId': FIXTURE['third']})
third.request('/clinic/queue', expected=401)
third.login(2, FIXTURE['prefix'] + '-third')
third.request('/clinic/queue')
temporary = 'Temporary-' + FIXTURE['password'][:12]
admin.request('/portal/admin/account-security/reset', {'role': 'PATIENT', 'accountId': FIXTURE['patient'], 'temporaryPassword': temporary})
patient.request('/clinic/queue', expected=401)
patient.login(2, password=temporary, forced=True)
patient.request('/clinic/queue', expected=403)
patient.request('/password/reset', {'currentPassword': temporary, 'newPassword': FIXTURE['password']})
patient.request('/clinic/queue', expected=401)
patient.login(2)
patient.request('/auth/logout', {})
patient.request('/clinic/queue', expected=401)
print(f'PASS: {CHECKS} real HTTP assertions against isolated Tomcat/OceanBase; no business account used.')
