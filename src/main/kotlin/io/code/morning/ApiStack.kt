package io.code.morning

import software.amazon.awscdk.core.App
import software.amazon.awscdk.core.Stack
import software.amazon.awscdk.core.StackProps
import software.amazon.awscdk.services.ec2.Vpc
import software.amazon.awscdk.services.ec2.VpcProps
import software.amazon.awscdk.services.ecr.Repository
import software.amazon.awscdk.services.ecs.*

class ApiStack @JvmOverloads constructor(app: App, id: String, props: StackProps? = null) :
    Stack(app, id, props) {

    init {
        // VPC
        val vpc = Vpc(
            this, "morning-code-api-vpc", VpcProps.builder()
                .cidr("10.0.0.0/16")
                .build()
        )

        // ECS Cluster
        val ecsCluster = Cluster(
            this, "morning-code-api-cluster", ClusterProps.builder()
                .vpc(vpc)
                .build()
        )

        // ECR
        val ecr = Repository(this, "morning-code-api")

        // TaskDefinition
        val taskDefinition =
            FargateTaskDefinition(
                this, "morning-code-api-fargate-task-definition",
                FargateTaskDefinitionProps.builder().build()
            )
                .addContainer(
                    "morning-code-api-container",
                    ContainerDefinitionOptions.builder().image(ContainerImage.fromEcrRepository(ecr)).build()
                ).taskDefinition

        // Fargate
        FargateService(
            this, "morning-code-api-fargate", FargateServiceProps.builder()
                .cluster(ecsCluster)
                .desiredCount(1)
                .taskDefinition(taskDefinition)
                .build()
        )
    }
}
