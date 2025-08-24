package com.pm.stack;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.msk.CfnCluster;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.route53.CfnHealthCheck;

import java.util.stream.Collectors;

public class LocalStack extends Stack {

    private final Vpc vpc;

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
