#!/usr/bin/bash

export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1

set -e # Stops the script if any command fails

aws --endpoint-url=http://localhost:4566 cloudformation delete-stack \
    --stack-name patient-management

aws --endpoint-url=http://localhost:4566 cloudformation wait stack-delete-complete \
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
