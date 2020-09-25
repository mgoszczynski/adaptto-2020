# adaptTo() 2020 - Assets and Components usage - Demo  

Project to support a demo for my adaptTo() presentation

# Installation
run `mvn clean install -PautoInstallPackage` to deploy to your AEM instance

#Usage
To collect component and assets usage data for We.Retail site just run

`curl -u admin:admin http://localhost:4502/bin/data-extract.json?path=/content/we-retail -o we-retail.json`
 
we-retail.json will contain extracted data. 