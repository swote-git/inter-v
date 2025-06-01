# infrastructure/variables.tf - Cognito 지원 변수들

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

# Cognito 관련 변수들
variable "cognito_user_pool_id" {
  description = "AWS Cognito User Pool ID"
  type        = string
  default     = ""
  validation {
    condition     = can(regex("^[a-zA-Z0-9_-]*$", var.cognito_user_pool_id))
    error_message = "Cognito User Pool ID must contain only alphanumeric characters, hyphens, and underscores."
  }
}

variable "cognito_client_id" {
  description = "AWS Cognito Client ID"
  type        = string
  default     = ""
  sensitive   = true
}

variable "cognito_client_secret" {
  description = "AWS Cognito Client Secret"
  type        = string
  default     = ""
  sensitive   = true
}

# 기존 변수들
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

# 선택적 변수들
variable "enable_cognito" {
  description = "Enable Cognito authentication"
  type        = bool
  default     = true
}

variable "instance_type" {
  description = "EC2 instance type"
  type        = string
  default     = "t3.small"
}

variable "rds_instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t3.micro"
}

variable "environment" {
  description = "Environment name (dev, staging, prod)"
  type        = string
  default     = "prod"
  validation {
    condition     = contains(["dev", "staging", "prod"], var.environment)
    error_message = "Environment must be one of: dev, staging, prod."
  }
}