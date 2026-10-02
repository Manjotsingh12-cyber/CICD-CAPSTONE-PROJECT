variable "aws_region" {
  default = "ap-south-1"
}

variable "project_name" {
  default = "billing-payment"
}

variable "vpc_cidr" {
  default = "10.0.0.0/16"
}
variable "image_tag" {
  description = "Docker image tag to deploy (git SHA)"
  default     = "latest"
}
