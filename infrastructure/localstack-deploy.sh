#!/usr/bin/bash

set -e # Stops the script if any command fails

aws --endpoint-url=http://localhost:4566 cloudformation delete-stack \
    --stack-name patient-management

# use aws cli to deploy cloud formation stack
# stack-name refers to the name of our stack
aws --endpoint-url=http://localhost:4566 cloudformation deploy \
    --stack-name patient-management \
    --template-file "./cdk.out/localstack.template.json"

# we need an endpoint for our loadbalancer url, where we can make requests to
#elbv2 -> elastic load balancer version 2
# query will print out the first load balancer's DNS name that we use to access at api gateway in our stack, and return it as text
# describe-load-balancers returns all load balancers and their addresses in our stack
aws --endpoint-url=http://localhost:4566 elbv2 describe-load-balancers \
    --query "LoadBalancers[0].DNSName" --output text

aws cloudformation describe-stack-events --stack-name patient-management