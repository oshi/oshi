# 🚀 Contributing to Oshi

Welcome to Oshi! 🎉 We're excited that you want to contribute to our project. We appreciate your help in making Oshi even more awesome. Here's how you can get started:

## 💡 How Can You Contribute?

Oshi welcomes various forms of contributions, including:

- 🐞 Reporting bugs
- 💡 Proposing new features
- 💻 Submitting code improvements
- 📖 Enhancing documentation
- 🌟 Fixing issues with "first-timers-only" tags
- and more!

If you're new to open source or need assistance with git, coding, or anything else, we're here to help! Look for issues with "first-timers-only" or "good first issue" tags. If you're unsure, just open a new issue and ask for guidance. We're happy to walk you through it.

## 🛠 Getting Started

To get started, ensure you have `git` and `maven` installed on your machine. For Windows, we recommend using [Git Bash](https://git-scm.com/downloads).

You can either install Maven or use the Maven wrapper by typing `./mvnw` instead of `mvn`.

## 🍴 Fork the Project

1. Fork the project on GitHub by clicking the "Fork" button in the top right corner of the project page.
2. Clone your fork to your local machine and set up a [triangle workflow](https://github.com/forwards/first-contributions/blob/master/additional-material/git_workflow_scenarios/keeping-your-fork-synced-with-this-repository.md) with these commands:

```shell
git clone https://github.com/yournamehere/oshi.git
cd oshi
git remote add upstream https://github.com/oshi/oshi.git
```

## 🌿 Create a Branch

Make sure your fork is up-to-date and create a branch for your feature or bug fix. The name `my-feature-branch` is just an example; choose a name you like.

```shell
git checkout master
git pull upstream master
git checkout -b my-feature-branch
```

## 🚦 Build and Test

Make sure you can build the project and run tests.

```shell
mvn test
```

Your changes should not break any existing tests. If you find a test failure unrelated to your changes, keep track of it, but make sure your code doesn't introduce new test failures. If you believe the test failure is a bug, create an issue!

## 🧪 Write Tests

For bug fixes, try to write a test that reproduces the problem you're fixing (even if it fails). For new features, write tests to ensure the feature works as intended.

We appreciate pull requests that highlight problems, even without a fix.

## 💻 Write Code

Now, it's time to implement your feature or bug fix.

Ensure that `mvn test` completes without errors. Use `mvn clean` if necessary.

## 📝 Update Changelog

The Changelog lets users know what's changed. Edit [CHANGELOG](CHANGELOG.md) to include your contribution under *Next Release*. Follow the format of other entries, including your name and a link to your GitHub account:

```markdown
* [#123](https://github.com/oshi/oshi/pull/123): Reticulated splines - [@contributor](https://github.com/contributor).
```

You can guess your pull request number as the next available number after issues and pull requests on the project.

## 🧹 Format Your Code

Run the following commands to format your code according to Oshi's conventions:

```shell
mvn spotless:apply
```

## 📜 Commit Changes

Make sure Git knows your name and email:

```shell
git config --global user.name "Your Name"
git config --global user.email "contributor@example.com"
```

Add your changed files to the index using [git add](https://git-scm.com/docs/git-add). Most IDEs provide an easy way to do this.

Write [good commit messages](https://chris.beams.io/posts/git-commit/). A commit message should describe what changed and why:

```shell
git add yourChangedFile.java
git commit -m "Fixed the Foo bug by changing bar"
```

## 🚀 Push to Your GitHub Repository

```shell
git push origin my-feature-branch
```

## 📥 Make a Pull Request

1. Go to https://github.com/yournamehere/oshi and select your feature branch.
2. Click the 'Pull Request' button and complete the form.
3. Pull requests are usually reviewed within a few days.

If code review requests changes (which often happens), simply `git push` the changes to your repository on the same branch, and the pull request will be updated automatically.

## ♻️ Rebase

If you've been working on your change for a while and other commits have been made to the project, rebase with `upstream/master`:

```shell
git fetch upstream
git rebase upstream/master
git push origin my-feature-branch
```

## 🔄 Update CHANGELOG Again

If you didn't guess the PR number right, update [CHANGELOG](CHANGELOG.md) with the correct pull request number.

You can either amend your previous commit and force push the changes or create a new commit. The maintainers can squash them later.

```shell
git commit --amend
git push origin my-feature-branch -f
```

## 🔍 Check on Your Pull Request

After a few minutes, return to your pull request and see if it passed the CI tests. Everything should look green. If not, read the failed test logs to identify issues, fix them, and commit as described above.

## 🕰 Be Patient

Your change may not be merged immediately, and maintainers might request more changes. Hang in there; we appreciate your hard work! 🙌

## 🙏 Thank You

Please know that we truly appreciate and value your time and effort. We love you! ❤️

Thank you for contributing to Oshi! 🎈

Happy coding! 🚀