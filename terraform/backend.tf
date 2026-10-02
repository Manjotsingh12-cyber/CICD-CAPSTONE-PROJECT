terraform {
  backend "s3" {
    bucket       = "billing-payment-lab-bucket-2-manjot-singh"
    key          = "cicd-capstone/ecs.tfstate"
    region       = "ap-south-1"
    use_lockfile = true
  }
}