-- Lost-Found status vocabulary migration.
-- Execute only in ry-vue-lostfound after taking a database backup.
-- Safe to rerun: updates only legacy status values.

USE `ry-vue-lostfound`;

START TRANSACTION;

UPDATE lf_claim
SET status = 'HANDOFF'
WHERE status = 'PARTIALLY_CONFIRMED'
  AND applicant_confirmed = '1'
  AND owner_confirmed = '0';

UPDATE lf_claim
SET status = 'ACCEPTED'
WHERE status = 'PARTIALLY_CONFIRMED'
  AND applicant_confirmed = '0'
  AND owner_confirmed = '1';

UPDATE lf_claim
SET status = 'COMPLETED'
WHERE status = 'PARTIALLY_CONFIRMED'
  AND applicant_confirmed = '1'
  AND owner_confirmed = '1';

UPDATE lf_claim
SET status = 'HANDOFF'
WHERE status = 'PARTIALLY_CONFIRMED';

UPDATE lf_match
SET status = 'CONFIRMED'
WHERE status = 'ACCEPTED';

UPDATE lf_match
SET status = 'IGNORED'
WHERE status = 'DISMISSED';

INSERT INTO lf_schema_version (version_code, description)
VALUES ('LF_STATUS_V2', '统一认领和匹配状态字典')
ON DUPLICATE KEY UPDATE description = VALUES(description);

COMMIT;

SELECT status, COUNT(*) AS total
FROM lf_claim
GROUP BY status
ORDER BY status;

SELECT status, COUNT(*) AS total
FROM lf_match
GROUP BY status
ORDER BY status;
