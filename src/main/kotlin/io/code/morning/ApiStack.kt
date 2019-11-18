package io.code.morning

import software.amazon.awscdk.core.App
import software.amazon.awscdk.core.Stack
import software.amazon.awscdk.core.StackProps
import software.amazon.awscdk.services.ec2.Instance
import software.amazon.awscdk.services.ec2.Vpc
import software.amazon.awscdk.services.ec2.VpcProps
import software.amazon.awscdk.services.ecr.Repository
import software.amazon.awscdk.services.ecr.RepositoryProps
import software.amazon.awscdk.services.ecs.*
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateServiceProps
import software.amazon.awscdk.services.servicediscovery.*

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
    // val ecr = Repository.fromRepositoryName(this, "morning-code-api-ecr", "morning-code-api")
    /*
    val ecr = Repository(
      this, "morning-code-api-ecr", RepositoryProps.builder()
        .repositoryName("morning-code-api-ecr-repository").build()
    )
     */
    //val containerImage = EcrImage.fromRegistry("morning-code-api")
    val ecrImageUri = this.node.tryGetContext("ecrImageUri").toString()
    val containerImage = EcrImage.fromRegistry(ecrImageUri)

    // TaskDefinition
    val taskDefinition =
      FargateTaskDefinition(
        this, "morning-code-api-fargate-task-definition",
        FargateTaskDefinitionProps.builder()
          .cpu(256)
          .memoryLimitMiB(1024)
          .build()
      )

    // ECS Log setting
    val awsLogDriver = AwsLogDriver(
      AwsLogDriverProps.builder()
        .streamPrefix("morning-code-api")
        .build()
    )

    // Container settings
    val appContainer = taskDefinition.addContainer(
      "morning-code-api-container",
      ContainerDefinitionOptions.builder()
        .image(containerImage)
//        .image(ContainerImage.fromEcrRepository(ecr))
        .logging(awsLogDriver)
        .build()
    )
    appContainer.addPortMappings(PortMapping.builder().containerPort(8080).build())

    // Fargate
    ApplicationLoadBalancedFargateService(
      this,
      "morning-code-api-fargate",
      ApplicationLoadBalancedFargateServiceProps.builder()
        .cluster(ecsCluster)
        .desiredCount(1)
        .taskDefinition(taskDefinition)
        .publicLoadBalancer(true)
        .build()
    )

    // Cloud Map
    /*
    val namespace = HttpNamespace(
      this,
      "morning-code-api-ns",
      HttpNamespaceProps.builder()
        .name("api.morning.code.io")
        .build()
    )

     */

    /*
    val service = Service(
      this,
      "morning-code",
      ServiceProps.builder()
        .namespace(namespace)
        .build()
    )
     */

    //val instance = Instance
  }
}
