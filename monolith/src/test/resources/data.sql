-- 测试种子数据（H2 内存库，每次测试上下文启动时自动执行）
INSERT INTO member (name, phone, gender, birthday, created_at) VALUES ('种子会员一', '13800000001', '男', '1995-06-01', CURRENT_TIMESTAMP);
INSERT INTO member (name, phone, gender, birthday, created_at) VALUES ('种子会员二', '13800000002', '女', '1998-03-15', CURRENT_TIMESTAMP);
