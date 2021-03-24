# Load tests

Other configuration is specific to each load test and passed as system
properties in `JAVA_OPTS` like the examples below.

# Running a load test locally

```shell
make run-locally

```

# Building the load test image

```shell
make build-image
```

## Publishing to quay.io

```shell
docker push quay.io/redhatdemo/2021-load-test
```

# Running in OpenShift

Copy `example.env` to `.env` and change the parameters

Run `make deploy-load-test` to deploy all the contents