# VCS
Version Control System CLI supporting basic git operations:

* `init` -- initialize a repository
* `add <files>` -- add the files to staging area
* `rm <files>` -- delete the files from repository
* `status` -- show changed/removed/untracked files
* `commit <message>` commit changes 
* `reset <to_revision>`. Behavior of `reset` is identical to `git reset --hard`
* `log [from_revision]` show commits with additional info (message, date and time, author) 
* `checkout <revision>`
    * Possible values of `revision`:
        * `commit hash` -- commit hash
        * `master` -- branch name
        * `HEAD~N`, where `N` is a positive number. `HEAD~N` is _Nth commit before HEAD (`HEAD~0 == HEAD`)
* `checkout -- <files>` -- restore changes in the files
