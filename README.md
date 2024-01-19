# Running your Quarkus App on Azure Functions

This tutorial shows you a simple Quarkus app that can be deployed on Azure Functions, refer to the [AZURE FUNCTIONS](https://quarkus.io/guides/azure-functions) and [Create your first containerized Azure Functions](https://learn.microsoft.com/azure/azure-functions/functions-deploy-container?tabs=acr%2Cbash%2Cazure-cli&pivots=programming-language-java) for more details.

## Prerequisites

To complete this tutorial, you need:

* JDK 11+ installed with *JAVA_HOME* configured appropriately.
* Apache Maven 3.9.6 installed.
* [An Azure Account](https://azure.microsoft.com/). Free accounts work.
* [Azure Functions Core Tools](https://learn.microsoft.com/azure/azure-functions/functions-run-local#v2) version 4.x installed.
* [Azure CLI](https://docs.microsoft.com/cli/azure/install-azure-cli) installed.

## Examine the project

[Function.java](https://github.com/majguo/quarkus-azure-functions-demo/blob/main/src/main/java/org/acme/Function.java) contains a simple HTTP trigger function that returns a greeting message. It's compiled to a JAR file and deployed to Azure Functions with Java runtime.

[GreetingResource.java](https://github.com/majguo/quarkus-azure-functions-demo/blob/main/src/main/java/org/acme/GreetingResource.java) is a JAX-RS resource that exposes a REST endpoint to return a greeting message. It's compiled to a native executable and deployed to Azure Functions with custom handler.

## Running the app with Functions App Java worker

Build the project with:

```shell script
mvn clean package
```

Run as a Function App with Java worker locally:

```shell script
mvn quarkus:run
```

Open another terminal and invoke the following command to verify if it works:

```shell script
curl -w "\n" -s http://localhost:8081/api/HttpExample
curl -w "\n" -s http://localhost:8081/api/HttpExample?name=quarkusapp
```

You should see the following output:

```shell output
Please pass a name on the query string or in the request body
Guten Tag quarkusapp
```

Switch back to the first terminal and press `Ctrl+C` to stop the application.

Log in to Azure with the Azure CLI:

```shell script
az login
```

Then run the following command to deploy the application to Azure Functions with Java worker:

```shell script
mvn quarkus:deploy
```

You should see the following message included in the output:

```shell output
[INFO] [io.quarkus.azure.functions.deployment.AzureFunctionsDeployCommand]       HttpExample : https://quarkus-azure-function-011924.azurewebsites.net/api/httpexample
```

Invoke the following command to verify if it works:

```shell script
curl -w "\n" -s https://quarkus-azure-function-011924.azurewebsites.net/api/httpexample
curl -w "\n" -s https://quarkus-azure-function-011924.azurewebsites.net/api/httpexample?name=quarkusapp
```

You should see the following output:

```shell output
Please pass a name on the query string or in the request body
Guten Tag quarkusapp
```
