ENV_FILE := .env
include ${ENV_FILE}
export $(shell sed 's/=.*//' ${ENV_FILE})

oc_login: 
	oc login ${OC_URL} -u ${OC_USER} -p ${OC_PASSWORD} --insecure-skip-tls-verify=true

build-image:
	sh build.sh
	docker build -t ${IMAGE} .

push-image:
	@echo Push Image
	docker push ${IMAGE}
	
delete-namespace: 
	oc delete project ${NAMESPACE} --ignore-not-found=true 

create-namespace: 
	oc new-project ${NAMESPACE}

run-locally:
	docker run --rm=true  -e ATTACKS=${ATTACKS} -e SOCKET_ADDRESS=${SOCKET_ADDRESS} -e USERS=${USERS}  -e SIMULATION=${SIMULATION} -e WS_PROTOCOL=${WS_PROTOCOL} ${IMAGE}

deploy-load-test:  
	oc project ${NAMESPACE}
	oc process -f openshift/template.yaml -p $ USERS=${USERS} NAMESPACE=${NAMESPACE} ATTACKS=${ATTACKS}  IMAGE=${IMAGE} REPLICAS=${REPLICAS} SOCKET_ADDRESS=${SOCKET_ADDRESS} SIMULATION=${SIMULATION} WS_PROTOCOL=${WS_PROTOCOL} | oc apply -f -


remove-load-test:
	oc project ${NAMESPACE}
	oc process -f openshift/template.yaml -p $ USERS=${USERS} NAMESPACE=${NAMESPACE} ATTACKS=${ATTACKS}  IMAGE=${IMAGE} REPLICAS=${REPLICAS} SOCKET_ADDRESS=${SOCKET_ADDRESS} SIMULATION=${SIMULATION} WS_PROTOCOL=${WS_PROTOCOL} | oc delete -f -

report:
	oc project ${NAMESPACE}
	oc get pods -o name | xargs -n 1 oc logs --tail=50 > logs/${NAMESPACE}.log