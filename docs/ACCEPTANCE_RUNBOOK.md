# Lost Found Acceptance Runbook

## Purpose

This runbook defines the final checks that cannot be proven by compilation alone.
Use the isolated `ry-vue-lostfound` database and the local services:

- Portal: `http://localhost:5173`
- Admin: `http://localhost`
- API: `http://localhost:8081`

Do not use the original `ry-vue` database for business acceptance.

## Role Acceptance

Use four controlled accounts, one for each role:

| Role | Required positive checks | Required negative checks |
| --- | --- | --- |
| `common` | Register or log in, publish own post, edit and withdraw own post, comment, message, report, appeal | Cannot open system user, role, menu, category-management, moderation-management or review actions |
| `lostfound_reviewer` | View review queue, inspect post content and images, approve or reject with reason, view match data | Cannot manage users, roles, menus, categories, sanctions or appeals |
| `lostfound_operator` | Manage categories, claims, reports, violation cases, sanctions and appeals | Cannot manage system users, roles or menus |
| `admin` | Access all required system and Lost-Found management functions | Must be restricted to trusted operators |

After changing a role, log out and log in again so the RuoYi permission cache
is refreshed. Validate both the menu visibility and the API response. A hidden
menu alone is not an access-control test.

Expected protected API result for a user without the permission is HTTP success
with RuoYi response code `403`; an unauthenticated request is returned with
RuoYi response code `401`.

## Post Workflow

With a `common` account:

1. Load the publish form and verify ITEM, COLOR and CAMPUS choices are present.
2. Confirm missing title, missing required category, invalid category IDs, wrong
   hierarchy and future event dates are rejected.
3. Create a post with text and at least one image.
4. Confirm the post is not public before review.
5. With a reviewer, inspect and approve it; verify the owner receives a notice
   and the post appears in the public square.
6. Repeat with rejection and verify the reason is visible to the owner.
7. Edit and resubmit the rejected post; verify no duplicate post is created.
8. Verify another ordinary user cannot edit, withdraw or inspect private draft
   content and images.

## Claim And Governance Workflow

1. Create a claim as the other party and verify the post owner can accept or
   reject it.
2. Confirm completion requires the intended two-party confirmation.
3. Repeat a claim or completion request and verify it is idempotent.
4. Submit reports for a post, comment, user, claim and private message.
5. Verify only the related party can submit claim or private-message reports.
6. Process a report into a violation case, apply a time-bounded restriction,
   and verify the restricted operation is blocked.
7. Submit and process an appeal; verify the decision and audit event are visible
   to the permitted operator.

## Backup And Restore

1. Stop write traffic before the backup.
2. In Navicat, back up `ry-vue-lostfound` as a complete structure-and-data SQL
   file.
3. Create a separate restore-test database and import the backup there.
4. In the restore-test database, run the read-only verification file
   `backend/sql/lost_found_restore_verify.sql`. Verify `sys_user`, `sys_role`,
   `sys_menu`, `sys_role_menu`, all `lf_*` tables, `lf_schema_version`,
   categories and sample posts are present.
5. Restore the upload directory represented by `ruoyi.profile` and verify
   `lf_post_image.object_key` files are readable.
6. Run the category prerequisite check against the restore-test environment
   and perform one login, post-detail and image-access smoke test.

Never test restoration by dropping or overwriting `ry-vue` or the active
`ry-vue-lostfound` database.
