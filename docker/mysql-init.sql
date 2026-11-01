-- 微服务专用 MySQL 初始化脚本（首次创建数据卷时自动执行）
-- 为三个微服务分别创建独立数据库和独立账号，从数据库层面强制服务边界：
-- 每个账号只能访问自己服务的库，不能跨服务读写数据表。

-- 1. member-service：会员档案库
CREATE DATABASE IF NOT EXISTS gym_member DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'member_u'@'%' IDENTIFIED BY 'member_pass';
GRANT ALL PRIVILEGES ON gym_member.* TO 'member_u'@'%';

-- 2. card-service：会员卡库（member_id 只是普通列，跨服务关联不建外键）
CREATE DATABASE IF NOT EXISTS gym_card DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'card_u'@'%' IDENTIFIED BY 'card_pass';
GRANT ALL PRIVILEGES ON gym_card.* TO 'card_u'@'%';

-- 3. checkin-service：入场记录库（member_id/card_id 均为普通列）
CREATE DATABASE IF NOT EXISTS gym_checkin DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'checkin_u'@'%' IDENTIFIED BY 'checkin_pass';
GRANT ALL PRIVILEGES ON gym_checkin.* TO 'checkin_u'@'%';

FLUSH PRIVILEGES;
