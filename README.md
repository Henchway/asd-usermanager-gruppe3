# User Manager

User Manager is an application which lets you register, login and change your password

## Table of contents

* [Group members](#group-members)
* [General info](#general-info)
* [Technologies](#technologies)
* [Setup](#setup)
* [Usage](#Usage)
    * [Register](#register)
    * [Login](#login)
    * [Change Password](#change-password)
    * [Delete Account](#delete-account)

<hr>  

## Group Members

* Marko Marjanovic
* Thomas Scheibelhofer
* Matthias Schmid-Kietreiber

## General infos

This project is part of the study course Advanced Software Development (SAD) on the FH-Campus Vienna with the purpose of
deepening the course material through practical exercises.
In no form is any monetization or commercialisation off this application intended!

## Technologies

Project is created with:

* Spring Boot
* Java 17

## Setup

To start the project follow the following steps:

* Open IntelliJ
* Add a 'Spring Boot' application configuration
* Run the application

## Usage

To use the functions of the application call the REST endpoints with the help of i.e. Postman or directly in the
provided interactive [Swagger documentation](http://localhost:8080/swagger-ui/index.html).

### Register

* /users/register

### Login

* /users/login
* /users/logout/{username}

### Change Password

* ...
* ...
* ...

### Delete Account

* /users/register
* /users/login
* /users/remove/{username}
