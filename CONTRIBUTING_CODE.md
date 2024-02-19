# Contribute Code

Your code can make a difference for the Data Sharing Framework (DSF). We invite all users to share their code, tests, scripts and improvement ideas. Contributions of any size enhance the DSF and support the data sharing framework community.

### Benefits of Contributing:
- Foster community growth and diversification.
- Sharpen your coding skills.
- Gain recognition in the DSF community.
- Directly impact the future of data sharing in medicine.

Start now by visiting our contribution pages. Every line of code helps us build a stronger and more versatile DSF.

## General

### Code Style

You can import our code style for your specific IDE:

* [Eclipse](https://github.com/datasharingframework/dsf/blob/main/src/main/resources/eclipse-formatter-config.xml). Open your preferences, click on `Java`, `Code style`, `Formatter`, `Import` and select the downloaded file.
* [IntelliJ](https://github.com/datasharingframework/dsf/blob/main/src/main/resources/intellij-formatter-config.xml). Open your settings, click on `Editor`, `Code style`, `Java`, the settings icon, `import scheme`, `IntelliJ` and select the downloaded file.

Pull requests will only be approved if the code is formatted according to the code style configurations above. To format the code with maven before pushing to GitHub, use `mvn compile -Pformat-and-sort`.

### Branching Strategy

[Git Flow](https://www.atlassian.com/de/git/tutorials/comparing-workflows/gitflow-workflow) is used as this project's branching strategy. Therefore, you will find the following structure:

* main
* develop
* issue
* hotfix
* release

Notice that only the first two elements listed are actual branches. The other elements are containers to hold all branches belonging to that category.


#### Branch Naming

The following ruleset is applied to name branches:

* `issue/<issue-number>_<issue-name>`
* `hotfix/<version>`
* `release/<version>`

## Setting up the Project

This chapter lists all important requirements to get the project buildable and running properly.

### Java

This project uses Java JDK 17, so make sure you have it installed on your system.

### Docker

[Docker](https://www.docker.com/) is used in this project to test database functionality and to run more complex test-setups.

### Maven

The project relies on [Maven](https://maven.apache.org/) as its management tool.
*Important:* When building the project you might encounter the following error:
*Could not determine gpg version* [GPG](https://gnupg.org/) is used to sign artifacts for public release. Since this does not concern contributors, you may skip this step in the maven build process with `-Dgpg.skip`.


## Workflow

1. Create an issue or comment on an issue that you want to contribute some feature
2. Fork the repository, create a branch and mention it in the issue
3. If you desire feedback, create a pull request or comment on it in the issue. Feel free to @ any member with write permissions if you feel like your request has not been registered yet. They will review your changes and/or change requests
4. If your changes are production-ready, create a [pull request](https://github.com/datasharingframework/dsf/pulls).

### Pull Request Process

We follow Martin Fowler's method for managing pull requests. This approach categorizes pull requests based on the level of trust and experience of the contributor, as well as the impact of the changes. Here's how we apply it:

1. **Ship**: For our most trusted contributors with a proven track record. These members can merge their pull requests without prior review, typically for minor or highly confident changes.

2. **Show**: This level is for trusted contributors who need some oversight, as well as for experienced developers who want to demonstrate how certain changes should be made in the future. They create pull requests and show their work to the team.

3. **Ask**: New or less experienced contributors, as well as those submitting more complex changes, fall into this category. They are required to ask for feedback and approval before their changes can be merged, ensuring thorough review and quality control.


This method helps us maintain a balance between code quality and efficient development, recognizing the varying levels of expertise among our contributors.

For more information on Fowler's approach, visit [Martin Fowler's article on Pull Requests](https://martinfowler.com/articles/ship-show-ask.html).


## Data Security in DSF Development

The DSF (Data Sharing Framework) and its process plugins are frequently used to transmit sensitive personal data. To prevent the release of personal data during development, please adhere to the following guidelines:

- **No development with real personal data:** Always use anonymized or synthetic data for development purposes.
- **No personal data in repositories:** Ensure no personal data is present in local and remote repositories intended for publication, not even temporarily.
- **Review all log files:** Before using log files in issues, examples, etc., thoroughly review them to ensure no personal and sensitive data is included.

