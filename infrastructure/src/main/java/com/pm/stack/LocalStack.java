package com.pm.stack;

import software.amazon.awscdk.*;

public class LocalStack extends Stack {

    // Extending AWS CDK's Stack class
    public LocalStack(final App scope, final String id, final StackProps props){
        super(scope, id, props);
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
