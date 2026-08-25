# Agent Documentation Checklist

**Purpose:** Ensure every AI-generated PR includes documentation that answers:
1. What was removed or deprecated?
2. What new features were added?
3. What breaking changes occurred?
4. What bugs were fixed?
5. How does this impact existing features?

## Before Creating a PR:

### For Architectural Changes (like PR #60)

- [ ] **Create a `CHANGE_SUMMARY.md`** in the PR root:
    - List all classes/methods marked @Deprecated
    - List all renamed fields (with find-replace mapping)
    - Explain why the change was necessary

- [ ] **Update affected documentation:**
    - [ ] `CLAUDE.md` if dev environment changed
    - [ ] `scenarios.md` if BDD scenarios changed
    - [ ] `PROJECT_PLAN.md` if architectural phases changed
    - [ ] API guide if endpoints changed

- [ ] **Add @Deprecated annotations:**
  ```java
  @Deprecated(since = "VERSION", forRemoval = true, message = "Link to PR or issue explaining replacement")