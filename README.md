## Introduction

This project is meant to showcase how to create a Spring Boot microservice that integrates with AWS S3 to perform CRUD
operations.<br>
The upload and download file operations are performed in a streaming fashion to support large files.

### Stack

- Gradle
- Java 17
- Spring Boot 3
- Spring Web
- Spring Cloud AWS
- TestContainers

### Features

#### Bucket Related

* verify if a bucket with a given name exists
* bucket creation
* delete bucket

#### Files related

* list bucket files names
* read file from bucket
* upload/overwrite file to bucket
* delete file from bucket
* generate pre-signed URL to share file

### App Configuration

`spring.servlet.multipart.max-file-size` and `spring.servlet.multipart.max-request-size` are configured to support files
as large as 500MB.
The download of the file in streaming is supported for up to 5 minutes, you can increase it by configure the
property `spring.mvc.async.request-timeout` (in milliseconds)

### Run the App

To run the app you have to set your AWS access keys in the `application.yml` file.
The bucket is automatically created at boot time if it doesn't already exists (this is done for your convenience, check
the name of the bucket in `application.yml` file).

### Use Postman as client
Please find in the project's root the `postman-collection.json` file

### TODO
1. replace generic exceptions with custom ones


