output "vpc_id" {
  value = aws_vpc.main.id
}

output "public_subnet_ids" {
  value = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  value = aws_subnet.private[*].id
}
output "app_url" {
  description = "URL used by the smoke test"
  value       = "http://${aws_instance.app.public_ip}:8000"
}
