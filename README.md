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

You have alraedy created a Function App with Java worker in the previous step. Now you can deploy the application as a Linux container to the existing App Service Plan.

First, go to [Azure Portal](https://portal.azure.com/) and you should see resource group *quarkus* is created. Go to that resource group, write down storage name for environment variables as below:

```shell script
RESOURCE_GROUP_NAME=quarkus
STORAGE_NAME=<storage account name>
```

Next, create an Azure Container Registry (ACR) instance for storing the container image:

```shell script

```shell script
ACR_NAME=<your unique container registry name>
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

Finally, deploy the container image to Azure Functions:

```shell script
PLAN_NAME=011924plan
az functionapp plan create \
    --resource-group ${RESOURCE_GROUP_NAME} \
    --name ${PLAN_NAME} \
    --location eastus \
    --number-of-workers 1 \
    --sku EP1 \
    --is-linux

QUARKUS_APP_NAME=011924quarkus
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
URL=$(az functionapp function show \
    --resource-group ${RESOURCE_GROUP_NAME} \
    --name $QUARKUS_APP_NAME \
    --function-name HttpExample \
    --query invokeUrlTemplate \
    -o tsv)
curl -w "\n" -s $URL
curl -w "\n" -s $URL?name=quarkusapp
```

You should see the following output:

```shell output
Please pass a name on the query string or in the request body
Guten Tag quarkusapp
```
