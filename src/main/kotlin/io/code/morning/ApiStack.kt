package io.code.morning

import software.amazon.awscdk.core.App
import software.amazon.awscdk.core.Duration
import software.amazon.awscdk.core.Stack
import software.amazon.awscdk.core.StackProps
import software.amazon.awscdk.services.ec2.Vpc
import software.amazon.awscdk.services.ec2.VpcProps
import software.amazon.awscdk.services.ecr.IRepository
import software.amazon.awscdk.services.ecr.Repository
import software.amazon.awscdk.services.ecs.*
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateServiceProps
import software.amazon.awscdk.services.servicediscovery.*
import software.amazon.awscdk.services.iam.ManagedPolicy

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
    val ecrArn: String = this.node.tryGetContext("ecrArn").toString()
    val repository: IRepository = Repository.fromRepositoryArn(this, "morning-code-api-ecr", ecrArn)
    val containerImage = EcrImage.fromEcrRepository(repository, "latest")

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
        .logging(awsLogDriver)
        .build()
    )
    appContainer.addPortMappings(PortMapping.builder().containerPort(8080).build())

    // Fargate
    val fargateService = ApplicationLoadBalancedFargateService(
      this,
      "FargateService",
      ApplicationLoadBalancedFargateServiceProps.builder()
        .cluster(ecsCluster)
        .desiredCount(1)
        .taskDefinition(taskDefinition)
        .publicLoadBalancer(true)
        .build()
    )

    // X-Ray
    val xray = fargateService.taskDefinition.addContainer(
      "x-ray-daemon",
      ContainerDefinitionOptions.builder()
        .image(ContainerImage.fromRegistry("amazon/aws-xray-daemon"))
        .entryPoint(mutableListOf("/usr/bin/xray", "-b", "0.0.0.0:2000", "-o"))
        .memoryReservationMiB(256)
        .logging(AwsLogDriver(AwsLogDriverProps.builder().streamPrefix("x-ray").build()))
        .essential(true)
        .build()
    )
    xray.taskDefinition.taskRole.addManagedPolicy(
      ManagedPolicy.fromAwsManagedPolicyName("AWSXRayDaemonWriteAccess")
    )
    xray.addPortMappings(
      PortMapping.builder()
        .hostPort(2000)
        .containerPort(2000)
        .protocol(Protocol.UDP)
        .build()
    )

    // Cloud Map
    val namespace = PrivateDnsNamespace(
      this,
      "NameSpace",
      PrivateDnsNamespaceProps.builder()
        .name("api.morningcode.io")
        .vpc(vpc)
        .build()
    )

    val service = namespace.createService(
      "Service",
      ServiceProps.builder()
        .namespace(namespace)
        .dnsRecordType(DnsRecordType.A_AAAA)
        .dnsTtl(Duration.seconds(30))
        .loadBalancer(true)
        .build()
    )

    service.registerLoadBalancer("LoadBalancer", fargateService.loadBalancer)
  }
}
