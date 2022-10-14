Contributing to Oshi
=====================

OSHI is work of [many contributors](https://github.com/oshi/oshi/graphs/contributors). You're encouraged to submit [pull requests](https://github.com/oshi/oshi/pulls), [propose features and discuss issues](https://github.com/oshi/oshi/issues).

OSHI is [first-timers-only](https://www.firsttimersonly.com/) friendly.  If you're new to open source, or coding, or git, we're happy to help you get started! Look for the `first-timers-only` or `good first issue` tags on issues, or simply post a new issue asking how you can help.  We'll walk you through the steps needed to contribute to the project.

### Install git and maven

If not installed, install `git` using your package manager. For Windows, [Git Bash](https://git-scm.com/downloads) is recommended.

You may either install maven or replace the below `mvn` calls with the maven wrapper, just typing `./mvnw` instead.

### Fork the Project

Fork the project on Github by clicking on the word "Fork" above and to the right of the main project page.  This will create your own fork at https://github.com/yournamehere/oshi.git.

Then clone your fork to your local repository on your machine and set up a [triangle workflow](https://github.com/forwards/first-contributions/blob/master/additional-material/git_workflow_scenarios/keeping-your-fork-synced-with-this-repository.md) using these commands:
```
git clone https://github.com/yournamehere/oshi.git
cd oshi
git remote add upstream https://github.com/oshi/oshi.git
```

### Create a Branch for your feature

Make sure your fork is up-to-date and create a branch for your feature or bug fix.  (The name `my-feature-branch` is an example. Choose whatever you like.)

```
git checkout master
git pull upstream master
git checkout -b my-feature-branch
```

### Build and Test

Ensure that you can build the project and run tests.

```
mvn test
```

After your change, the tests should still pass.

It is possible that some tests may fail on your operating system or hardware for conditions unrelated to your change. In this case, keep track of which test(s) failed and make sure your code does not make any additional tests fail. If you believe the test failure represents a bug elsewhere, submit an issue!

### Write Tests

For bug fixes, try to write a test that reproduces the problem you're trying to fix (and fails).
For new features, write a test that produces results for a feature that you want to build.

We definitely appreciate pull requests that highlight or reproduce a problem, even without a fix.

### Write Code

Implement your feature or bug fix.

Make sure that `mvn test` completes without errors. (Use `clean` if necessary.)

### Update Changelog

The Changelog lets users know whether they should update to the latest version.  Editing the changelog is optional for minor bug fixes that are not user-facing, but should be added for new features.

Add a line to [CHANGELOG](CHANGELOG.md) under *Next Release*. Make it look like every other line, including your name and link to your Github account. A typical entry looks as follows.

```
* [#123](https://github.com/oshi/oshi/pull/123): Reticulated splines - [@contributor](https://github.com/contributor).
```
Note that the change log will link to your pull request number, which you don't know yet but can guess as the next higher number of Issues or Pull Requests on the project that has not been used.

### Format your code

Run the following commands to fix up your license header and align your code with OSHI's formatting conventions
```
mvn spotless:apply
```

### Commit Changes

Make sure git knows your name and email address.

```
git config --global user.name "Your Name"
git config --global user.email "contributor@example.com"
```

Add the changed files to the index using [git add](https://git-scm.com/docs/git-add).  Most IDEs make this easy for you to do, so you won't need this command line version.

Writing [good commit logs](https://chris.beams.io/posts/git-commit/) is important. A commit log should describe what changed and why.

```
git add yourChangedFile.java
git commit -m "Fixed Foo bug by changing bar"
```

### Push to your GitHub repository

```
git push origin my-feature-branch
```

#### Make a Pull Request

Go to https://github.com/yournamehere/oshi and select your feature branch. Click the 'Pull Request' button and fill out the form. Pull requests are usually reviewed within a few days.

If code review requests changes (and it usually will) just `git push` the changes to your repository on the same branch, and the pull request will be automatically updated.

### Rebase

If you've been working on a change for a while and other commits have been made to the project, rebase with upstream/master.

```
git fetch upstream
git rebase upstream/master
git push origin my-feature-branch
```

### Update CHANGELOG Again

If you didn't guess right on the PR number, update the [CHANGELOG](CHANGELOG.md) with the pull request number.

You may amend your previous commit and force push the changes, or just submit a new commit; the maintainers can squash them later.

```
git commit --amend
git push origin my-feature-branch -f
```

### Check on Your Pull Request

Go back to your pull request after a few minutes and see whether it passed muster with the CI tests.
Everything should look green, otherwise read the failed test logs to identify failed tests or compile errors.
Fix issues and commit again as described above.

### Be Patient

It's likely that your change will not be merged and that the nitpicky maintainers will ask you to do more, or fix seemingly benign problems like [choices of variable names](https://quotesondesign.com/phil-karlton/). Hang in there!

### Thank You

Please do know that we really appreciate and value your time and work. We love you, really.
