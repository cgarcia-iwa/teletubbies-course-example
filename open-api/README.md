## Requirements

### Make sure you have installed the following tool in order to run the web application successfully.

- `node` - v20.x.x
- `npm` - v10.x.x

### How to Generate the bundled openapi.yaml

If you have multiple reference files in your OpenAPI specification and need to create a single YAML file for RapiDoc, you can use `swagger-cli` to bundle all the references.

`npm install`

To generate the openAPI bundle file, run the following command:

`npm run build`

## How to run

To run the web application, execute the following command:

`npm run start`

Once the server is running, open your web browser and go to:

```shell script
http://127.0.0.1:8080
```

The RapiDoc OpenAPI spec viewer will load and display the API documentation using the specs located in the /openapi/build/ directory.
