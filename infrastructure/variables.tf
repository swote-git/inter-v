# infrastructure/variables.tf - 필수 변수들만

variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "ap-northeast-2"
}

variable "app_name" {
  description = "Application name"
  type        = string
  default     = "interv"
}

variable "domain_name" {
  description = "Domain name"
  type        = string
  default     = "interv.swote.dev"
}

variable "key_pair_name" {
  description = "EC2 Key Pair name"
  type        = string
}

variable "db_password" {
  description = "RDS database password"
  type        = string
  sensitive   = true
}

variable "use_existing_vpc" {
  description = "Use existing VPC instead of creating new one"
  type        = bool
  default     = true
}

variable "use_existing_eips" {
  description = "Use existing EIPs instead of creating new ones"  
  type        = bool
  default     = true
}