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

## Running the Quarkus app with Functions App Java worker

First of all, you leverage Quarkus maven plugin to run the app locally and remotely with Functions App Java worker.

### Run the app locally

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

### Deploy the app to Azure Functions

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

### Deploy the app to Azure Functions as a Linux container

You have already created a Function App with Java worker in the previous step. Now you can deploy the application as a Linux container to the existing App Service Plan.

First, go to [Azure Portal](https://portal.azure.com/) and you should see resource group *quarkus* is created. Go to that resource group, write down storage name. 
Then switch back to the terminal where you ran the application, and define the following environment variables with the value you wrote down previously:

```shell script
RESOURCE_GROUP_NAME=quarkus
STORAGE_NAME=<storage account name>
```

Next, create an Azure Container Registry (ACR) instance with a unique name (e.g., *mjg011924acr*) for storing the container image:

```shell script

```shell script
ACR_NAME=mjg011924acr
az acr create \
    --resource-group $RESOURCE_GROUP_NAME \
    --name $ACR_NAME \
    --sku Basic \
    --admin-enabled
```

Retrieve the ACR login information:

```shell script
ACR_LOGIN_SERVER=$(az acr show \
    --name $ACR_NAME \
    --query 'loginServer' \
    --output tsv)
ACR_USER_NAME=$(az acr credential show \
    --name $ACR_NAME \
    --query 'username' \
    --output tsv)
ACR_PASSWORD=$(az acr credential show \
    --name $ACR_NAME \
    --query 'passwords[0].value' \
    --output tsv)
```

Then, build and push the container image which supports Java worker to ACR:

```shell script
az acr build \
    --registry ${ACR_NAME} \
    --image azurefnquarkusimage:v1.0.0 \
    --file=Dockerfile \
    target/azure-functions/quarkus-azure-function-011924
```

Pull the image and run it locally to verify if it works:

```shell script
az acr login -n ${ACR_NAME}
docker run -p 8081:80 -it ${ACR_LOGIN_SERVER}/azurefnquarkusimage:v1.0.0
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

Switch back to the first terminal and press `Ctrl+C` to stop the container.
Run the following commands to deploy the container image to Azure Functions:

```shell script
PLAN_NAME=mjg011924plan
az functionapp plan create \
    --resource-group ${RESOURCE_GROUP_NAME} \
    --name ${PLAN_NAME} \
    --location eastus \
    --number-of-workers 1 \
    --sku EP1 \
    --is-linux

QUARKUS_APP_NAME=mjg011924quarkusimage
az functionapp create \
    --name $QUARKUS_APP_NAME \
    --storage-account $STORAGE_NAME \
    --resource-group ${RESOURCE_GROUP_NAME} \
    --plan ${PLAN_NAME} \
    --image ${ACR_LOGIN_SERVER}/azurefnquarkusimage:v1.0.0 \
    --registry-username $ACR_USER_NAME \
    --registry-password $ACR_PASSWORD \
    --runtime java
```

Wait for a while until the Function App is running, then invoke the following command to verify if it works:

```shell script
QUARKUS_APP_URL=$(az functionapp function show \
    --resource-group ${RESOURCE_GROUP_NAME} \
    --name $QUARKUS_APP_NAME \
    --function-name HttpExample \
    --query invokeUrlTemplate \
    -o tsv)
curl -w "\n" -s $QUARKUS_APP_URL
curl -w "\n" -s $QUARKUS_APP_URL?name=quarkusapp
```

You should see the following output:

```shell output
Please pass a name on the query string or in the request body
Guten Tag quarkusapp
```

## Running the Quarkus native app with Functions App custom handler

In this section, you build the Quarkus app to a native exectuable, run the native executable locally and remotely with Functions App custom handler.

### Run the native app locally

Build the app as a native executable:

```shell script
mvn package -Pnative -Dquarkus.native.container-build=true
```

Run the exectuable locally:

```shell script
./target/quarkus-azure-function-1.0.0-SNAPSHOT
```

Open another terminal and invoke the following command to verify if the REST endpoint defined in the JAX-RS resource returns two predefined objects:

```shell script
curl -w "\n" -s http://localhost:8080/api/HttpExample
```

You should see the following output:

```shell output
[{"name":"Apple"},{"name":"Pineapple"}]
```

Switch back to the first terminal and press `Ctrl+C` to stop the application.

Copy the native executable to the directory where the deployment descriptor files for the custom handler are prepared, and run it as a Function app locally:

```shell script
cp target/quarkus-azure-function-1.0.0-SNAPSHOT quarkus-azure-function-native/
cd quarkus-azure-function-native
func host start -p 8081
```

Open another terminal and invoke the following command to verify if it works:

```shell script
curl -w "\n" -s http://localhost:8081/api/HttpExample
```

You should see the following output:

```shell output
[{"name":"Apple"},{"name":"Pineapple"}]
```

Switch back to the first terminal and press `Ctrl+C` to stop the application.

### Deploy the native app to Azure Functions

Now run the following command to create a new Azure Functions app and publish the Quarkus native app to it:

```shell script
QUARKUS_NATIVE_APP_NAME=mjg011924quarkusnative
az functionapp create \
    --name $QUARKUS_NATIVE_APP_NAME \
    --storage-account $STORAGE_NAME \
    --resource-group ${RESOURCE_GROUP_NAME} \
    --plan ${PLAN_NAME} \
    --os-type Linux \
    --runtime custom

func azure functionapp publish ${QUARKUS_NATIVE_APP_NAME}
```

You should see the following message included in the output:

```shell output
[INFO] [io.quarkus.azure.functions.deployment.AzureFunctionsDeployCommand]       HttpExample : https://quarkus-azure-function-011924.azurewebsites.net/api/httpexample
```

Invoke the following command to verify if it works:

```shell script
QUARKUS_NATIVE_APP_URL=$(az functionapp function show \
    --resource-group ${RESOURCE_GROUP_NAME} \
    --name $QUARKUS_NATIVE_APP_NAME \
    --function-name HttpExample \
    --query invokeUrlTemplate \
    -o tsv | sed s/{[*]path}/HttpExample/g)
curl -w "\n" -s $QUARKUS_NATIVE_APP_URL
```

You should see the following output:

```shell output
[{"name":"Apple"},{"name":"Pineapple"}]
```

### Deploy the native app to Azure Functions as a Linux container

You have already created a Function App with custom handler in the previous step. Now you can deploy the native app as a Linux container to the existing App Service Plan.

First, build and push the container image which supports custom handler to ACR:

```shell script
cd ..
az acr build \
    --registry ${ACR_NAME} \
    --image azurefnquarkusnativeimage:v1.0.0 \
    --file=Dockerfile-custom \
    quarkus-azure-function-native
```

Pull the image and run it locally to verify if it works:

```shell script
docker run -p 8081:80 -it ${ACR_LOGIN_SERVER}/azurefnquarkusnativeimage:v1.0.0
```

Open another terminal and invoke the following command to verify if it works:

```shell script
curl -w "\n" -s http://localhost:8081/api/HttpExample
```

You should see the following output:

```shell output
[{"name":"Apple"},{"name":"Pineapple"}]
```

Switch back to the first terminal and press `Ctrl+C` to stop the container.
Run the following commands to deploy the container image to Azure Functions:

```shell script
QUARKUS_NATIVE_APP_IMAGE_NAME=mjg011924quarkusnativeimage
az functionapp create \
    --name $QUARKUS_NATIVE_APP_IMAGE_NAME \
    --storage-account $STORAGE_NAME \
    --resource-group ${RESOURCE_GROUP_NAME} \
    --plan ${PLAN_NAME} \
    --image ${ACR_LOGIN_SERVER}/azurefnquarkusnativeimage:v1.0.0 \
    --registry-username $ACR_USER_NAME \
    --registry-password $ACR_PASSWORD \
    --runtime custom
```

Wait for a while until the Function App is running, then invoke the following command to verify if it works:

```shell script
QUARKUS_NATIVE_APP_IMAGE_URL=$(az functionapp function show \
    --resource-group ${RESOURCE_GROUP_NAME} \
    --name $QUARKUS_NATIVE_APP_IMAGE_NAME \
    --function-name HttpExample \
    --query invokeUrlTemplate \
    -o tsv | sed s/{[*]path}/HttpExample/g)
curl -w "\n" -s $QUARKUS_NATIVE_APP_IMAGE_URL
```

You should see the following output:

```shell output
[{"name":"Apple"},{"name":"Pineapple"}]
```

## Clean up resources

Run the following command to delete the resource group and all resources created in this tutorial:

```shell script
az group delete --name $RESOURCE_GROUP_NAME --yes --no-wait
```

## References

Here're some useful references:

* [quarkus-azure-functions-blob](https://github.com/charlesamoreira/quarkus-azure-functions-blob): A Quarkus app that can be deployed on Azure Functions with custom handler with Blob Trigger.
* [Azure Functions Golang Worker](https://github.com/radu-matei/azure-functions-golang-worker): A Golang worker for Azure Functions.
* [Language Extensibility](https://github.com/Azure/azure-functions-host/wiki/Language-Extensibility): Azure Functions language extensibility options.
* [Quickstart: Create a Java function in Azure from the command line](https://learn.microsoft.com/azure/azure-functions/create-first-function-cli-java?tabs=windows%2Cbash%2Cazure-cli%2Cbrowser)
* [Working with containers and Azure Functions](https://learn.microsoft.com/azure/azure-functions/functions-how-to-custom-container?tabs=core-tools%2Cacr%2Cazure-cli&pivots=azure-functions)
* [Source code to generate deployment descriptor files in Quarkus Azure Functions extension](https://github.com/quarkusio/quarkus/blob/main/extensions/azure-functions/deployment/src/main/java/io/quarkus/azure/functions/deployment/AzureFunctionsProcessor.java)

---

## Scratch notes from 2024-01-22

Consider this example [Azure Blob storage trigger for Azure Functions](https://learn.microsoft.com/en-us/azure/azure-functions/functions-bindings-storage-blob-trigger?tabs=python-v2%2Cisolated-process%2Cnodejs-v4&pivots=programming-language-java#example). This example works in JVM mode, with the Java Worker, but does not work when compiled into native code.

### Why does Ed think it works in JVM mode?

- The `azure-functions-maven-plugin` causes some code do be invoked that discovers the java code has contains a `@BlobTrigger` annotation and generates the binding JSON file that is somehowe consumed by the Java worker, causing the correct annotated method to be invoked in response to the blob event. I think [this code](https://github.com/microsoft/azure-maven-plugins/blob/develop/azure-toolkit-libs/azure-toolkit-appservice-lib/src/main/java/com/microsoft/azure/toolkit/lib/legacy/function/bindings/BindingEnum.java) may have something to do with it.

### Why does Ed think it does not work in native mode?

- There is a missing piece when going from JVM mode to native mode. When you go to native mode, you must use a [custom handler](https://learn.microsoft.com/en-us/azure/azure-functions/functions-custom-handlers). The missing piece is some kind of dispatcher that calls a rest endpoint that ultimately calls the java method with the `@BlobTrigger` annotation.

### What is one way we can make it work in native mode?

- We could write a proper [AnnotationProcessor](https://docs.oracle.com/en/java/javase/21/docs/api/java.compiler/javax/annotation/processing/AbstractProcessor.html) that generates
   - the dispatcher JSON.
   - A customer dispatcher native code, like [this](https://github.com/Azure-Samples/functions-custom-handlers/blob/d228dc00f1c95d793d23f47e2a0071308a21437b/go/GoCustomHandlers.go#L207).
   - A Java rest endpoint that is called by the native code. When it receives the call from the native code, it invokes the java method with the `@BlobTrigger` annotation.
