package com.pm.stack;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.msk.CfnCluster;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.route53.CfnHealthCheck;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LocalStack extends Stack {

    private final Vpc vpc;
    private final Cluster ecsCluster;

    // Extending AWS CDK's Stack class
    public LocalStack(final App scope, final String id, final StackProps props){
        super(scope, id, props);
        this.vpc = createVpc();

        // create auth service db instance
        DatabaseInstance authServiceDb =
                createDatabase("AuthServiceDB", "auth-service-db");

        // create patient service db instance
        DatabaseInstance patientServiceDb =
                createDatabase("PatientServiceDB", "patient-service-db");

        // create auth service db health check
        CfnHealthCheck authDbHealthCheck = createDbHealthCheck(authServiceDb, "AuthServiceDBHealthCheck");

        // create patient service db health check
        CfnHealthCheck patientDbHealthCheck = createDbHealthCheck(patientServiceDb, "PatientServiceDbHealthCheck");

        CfnCluster mskCluster = createMskCluster();

        this.ecsCluster = createEscCluster();

        FargateService authService =
                createFargateService("AuthService", "auth-service",
                        List.of(4005),
                        authServiceDb,
                        Map.of("JWT_SECRET", "Q/EOWCwSG2+eRSiVzoYh7i0vx9C1BBFHgJAxWCzBqMuYh70nX9yjYQE+Z22yYLWn"));

        // below 2 lines tells CDK that authService depends on authDbHeathCheck and authServiceDB
        // it makes aure database starts before the auth service
        authService.getNode().addDependency(authDbHealthCheck);
        authService.getNode().addDependency(authServiceDb);

        FargateService billingService =
                createFargateService("BillingService", "billing-service",
                        List.of(4001, 9001),
                        null,
                        null);

        FargateService analyticsService =
                createFargateService("AnalyticsService", "analytics-service",
                        List.of(4002),
                        null,
                        null);

        // analytics service has a dependency on kafka cluster
        analyticsService.getNode().addDependency(mskCluster);

        FargateService patientService =
                createFargateService("PatientService", "patient-service",
                        List.of(4000),
                        patientServiceDb,
                        Map.of("BILLING_SERVICE_ADDRESS", "host.docker.internal",
                                "BILLING_SERVICE_GRPC_PORT", "9001"
                        ));

        // patient service depends on patientServiceDb, patientDbHealthCheck, billingService and kafka mskCluster
        patientService.getNode().addDependency(patientServiceDb);
        patientService.getNode().addDependency(patientDbHealthCheck);
        patientService.getNode().addDependency(billingService);
        patientService.getNode().addDependency(mskCluster);

        createApiGatewayService();

    }

    // Create a virtual private cloud
    private Vpc createVpc(){
       //AWS CDK takes this code and properties and converts it to cloud formation code
        return Vpc.Builder.create(this, "PatientManagementVPC")
                .vpcName("PatientManagementVPC")
                .maxAzs(2) // Availability zones (maxAzs)
                .build();
    }


    // Create a new database
    private DatabaseInstance createDatabase(String id, String dbName){
        return DatabaseInstance.Builder.create(this, id)
                .engine(DatabaseInstanceEngine
                        .postgres(PostgresInstanceEngineProps
                                .builder()
                                .version(PostgresEngineVersion.VER_17_2)
                                .build()))
                .vpc(vpc)
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO))
                .allocatedStorage(20)
                .credentials(Credentials.fromGeneratedSecret("postgres"))
                .databaseName(dbName)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();
    }

    // Health check for database instances
    private CfnHealthCheck createDbHealthCheck(DatabaseInstance db, String id){
        return CfnHealthCheck.Builder
                .create(this, id)
                .healthCheckConfig(CfnHealthCheck.HealthCheckConfigProperty.builder()
                        .type("TCP") // use TCP endpoint to check if database is online
                        .port(Token.asNumber(db.getDbInstanceEndpointPort())) // get the port at which DB is running, convert it to a number and pass it to CDK
                        .ipAddress(db.getDbInstanceEndpointAddress()) // tells health check function, which address to look for
                        .requestInterval(30) // check the status of DB every 30 seconds
                        .failureThreshold(3) // check 3 times before reporting failure
                        .build())
                .build();
    }

    private CfnCluster createMskCluster(){
        return CfnCluster.Builder.create(this, "MskCluster")
                .clusterName("kafka-cluster")
                .kafkaVersion("2.8.0")
                .numberOfBrokerNodes(1)
                .brokerNodeGroupInfo(CfnCluster.BrokerNodeGroupInfoProperty.builder()
                        .instanceType("kafka.m5.xlarge") // instance type: type of machine to run on -> compute power, memory etc
                        .clientSubnets(vpc.getPrivateSubnets().stream() // get list of private subnets from vpc and pass it to client subnets argument
                                .map(ISubnet::getSubnetId)
                                .collect(Collectors.toList()))
                .brokerAzDistribution("DEFAULT").build()) // distribution across availability zones
                .build();
    }

    private Cluster createEscCluster(){
        return Cluster.Builder.create(this, "PatientManagementCluster")
                .vpc(vpc) // connect cluster to vpc
                .defaultCloudMapNamespace(CloudMapNamespaceOptions.builder() //CloudMapNamespace helps to map the cluster to a namespace so we dont have to remember the IP addresses off all clusters.
                                .name("patient-management.local")
                        .build())
                .build();
    }

    // FargateService - Manages ECS tasks. Helps in starting/stopping ECS tasks in different containers
    // This method takes in a ID, image Name (of container), list of ports to expose, a database instance to connect to (if any),
    // and a map of additional environment variables for the container
    // The method then creates a container with the provided configs.
    private FargateService createFargateService(String id,
                                                String imgName,
                                                List<Integer> ports,
                                                DatabaseInstance db,
                                                Map<String, String> additionalEnvVars) {

        FargateTaskDefinition taskDefinition = FargateTaskDefinition.Builder.create(this, id + "Task")
                .cpu(256)
                .memoryLimitMiB(512)
                .build();

        // Container image will be pulled from ECR and container will be built according to it
        ContainerDefinitionOptions.Builder containerOptions = ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromRegistry(imgName))
                .portMappings(ports.stream()
                        .map(port -> PortMapping.builder()
                                .containerPort(port)
                                .hostPort(port)
                                .protocol(Protocol.TCP)
                                .build())
                        .toList())
                //below few lines help to create a log group with the name ecs + imgName
                .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                .logGroup(LogGroup.Builder.create(this, id + "LogGroup")
                                .logGroupName("/ecs/" + imgName)
                                        // whenever stack is destroyed, logs are also destroyed
                                        .removalPolicy(RemovalPolicy.DESTROY)
                                        // how long logs are stored
                                        .retention(RetentionDays.ONE_DAY)
                                .build())
                                .streamPrefix(imgName)
                        .build()));

        // environment variables for the container
        Map<String, String> envVars = new HashMap<>();
        // addresses of kafka servers. to be used by other services when communicating with kafka
        envVars.put("SPRING_KAFKA_BOOTSTRAP_SERVERS", "localhost.localstack.cloud:4510, localhost.localstack.cloud:4511, localhost.localstack.cloud:4512");

        if(additionalEnvVars != null)
            envVars.putAll(additionalEnvVars);

        // connect to the database of the particular service.
        if(db != null){
            envVars.put("SPRING_DATASOURCE_URL", "jdbc:postgresql://%s:%s/%s-db".formatted(
                    db.getDbInstanceEndpointAddress(),
                    db.getDbInstanceEndpointPort(),
                    imgName
            ));

            envVars.put("SPRING_DATASOURCE_USERNAME", "admin_user");
            envVars.put("SPRING_DATASOURCE_PASSWORD",
                    db.getSecret().secretValueFromJson("password").toString());

            envVars.put("SPRING_JPA_HIBERNATE_DDL_AUTO", "update");
            envVars.put("SPRING_SQL_INIT_MODE", "always");
            // try to connect to db for 60 seconds using retries before declaring a failure
            envVars.put("SPRING_DATASOURCE_HIKARI_INITIALIZATION_FAIL_TIMEOUT", "60000");
        }

        containerOptions.environment(envVars);
        // add a new container to task definition with above configs
        taskDefinition.addContainer(imgName + "Container", containerOptions.build());

        return FargateService.Builder.create(this, id)
                .cluster(ecsCluster)
                .taskDefinition(taskDefinition)
                .assignPublicIp(false)
                .serviceName(imgName)
                .build();
    }

    private void createApiGatewayService(){
        FargateTaskDefinition taskDefinition = FargateTaskDefinition.Builder.create(this, "APIGatewayTaskDefinition")
                .cpu(256)
                .memoryLimitMiB(512)
                .build();
        ContainerDefinitionOptions containerOptions = ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromRegistry("api-gateway"))
                // Spring profiles active basically tells cdk which yaml file to use
                // here we mention prod, which means this is for the production environment
                // in the api-gateway service, we have 2 yaml files for regular development and prod
                // having this configuration tells cdk to use the prod yaml file
                .environment(Map.of(
                        "SPRING_PROFILES_ACTIVE", "prod",
                        "AUTH_SERVICE_URL", "http://host.docker.internal:4005"
                ))
                .portMappings(List.of(4004).stream()
                        .map(port -> PortMapping.builder()
                                .containerPort(port)
                                .hostPort(port)
                                .protocol(Protocol.TCP)
                                .build())
                        .toList())
                //below few lines help to create a log group with the name ecs + imgName
                .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                        .logGroup(LogGroup.Builder.create(this, "ApiGatewayLogGroup")
                                .logGroupName("/ecs/api-gateway")
                                // whenever stack is destroyed, logs are also destroyed
                                .removalPolicy(RemovalPolicy.DESTROY)
                                // how long logs are stored
                                .retention(RetentionDays.ONE_DAY)
                                .build())
                        .streamPrefix("api-gateway")
                        .build()))
                .build();

        // link container options to task definition
        taskDefinition.addContainer("APIGatewayContainer", containerOptions);

        ApplicationLoadBalancedFargateService apiGateway =
                ApplicationLoadBalancedFargateService.Builder.create(this, "APIGatewayService")
                        .cluster(ecsCluster)
                        .serviceName("api-gateway")
                        .taskDefinition(taskDefinition)
                        // desiredCount -> number of instances
                        .desiredCount(1)
                        // Application Load Balancer will wait for 60 seconds for Api Gateway container to start before throwing error
                        .healthCheckGracePeriod(Duration.seconds(60))
                        .build();
    }

    public static void main(final String[] args){
        // creating new CDK app and defining where we want our output to be
        App app = new App(AppProps.builder().outdir("./cdk.out").build());

        // additional properties to apply to stack. synthesizer is used to convert code to cloud formation template
        // BootstraplessSynthesizer -> telling cdk to skip initial bootstrapping
        StackProps props = StackProps.builder()
                .synthesizer(new BootstraplessSynthesizer())
                .build();

        new LocalStack(app, "localstack", props);

        // tells cdk to take our stack, add props (additional properties), convert it to cloud formation template and put everything in cdk.out folder
        app.synth();

        System.out.println("App synthesizing in progress ... ");
    }

}
