# 🏥 Patient Management System

A scalable and modular **Patient Management System** built with a microservice architecture for handling healthcare workflows. Designed for flexibility, high availability, and performance, the system leverages modern cloud-native technologies and communication patterns to streamline patient data management, authentication, and inter-service coordination.

---

## 🚀 Features

* **Microservices Architecture**
  Decoupled services handling core functions like patient records, authentication, appointments, and notifications.

* **Database**
  PostgreSQL used as the primary relational database for structured and transactional data storage.

* **Cloud Deployment**
  Deployed on **AWS** using **Docker** and **Kubernetes**, enabling scalable container orchestration.

* **Inter-Service Communication**

  * **HTTP** for standard RESTful interactions
  * **gRPC** for high-performance, low-latency communication between microservices

* **Asynchronous Messaging**
  Integrated with **Apache Kafka** to manage event-driven communication and decoupled data flow.

* **Local Development Support**
  Used **LocalStack** to simulate AWS services locally for efficient development and testing.

* **Infrastructure as Code**
  Configured infrastructure components using **AWS CloudFormation** for reproducible deployments.

* **AWS Services**

  * Configured **ECS clusters**, **API Gateway**, and **Application Load Balancers (ALB)**
  * Implemented secure and scalable services for authentication and access control

* **API Documentation**
  Generated using **OpenAPI Specification** for standardized and interactive API verification.

* **Testing**
  Performed thorough **unit** and **integration testing** across all services to ensure reliability.

---

## 🧱 Architecture Overview

```
+--------------------+         +--------------------+         +---------------------+
| Authentication Svc | <--->   |  Patient Svc       | <--->   |  Appointments Svc   |
+--------------------+         +--------------------+         +---------------------+
         |                             |                               |
         v                             v                               v
     +--------+                  +----------+                     +-----------+
     | Kafka  |  <------------>  | PostgreSQL| <----------------> | LocalStack |
     +--------+                  +----------+                     +-----------+

             |                                                        |
             v                                                        v

     +------------------+                                   +---------------------+
     | Load Balancer /  |  <--------->  Kubernetes Cluster  |  API Gateway        |
     | API Gateway      |                                   +---------------------+
     +------------------+
```

---

## 🧪 Testing

* **Unit Tests**: Validate core logic at the service level.
* **Integration Tests**: Ensure reliable communication across microservices.

---

## 📦 Tech Stack

* **Backend**: Spring Boot (Java)
* **Database**: PostgreSQL
* **Queueing**: Apache Kafka
* **Deployment**: Docker, Kubernetes, AWS ECS
* **Infrastructure**: AWS CloudFormation, LocalStack
* **API Communication**: HTTP, gRPC
* **Docs**: OpenAPI (Swagger)

---

## 🛠️ Setup Instructions

> *Note: Detailed setup scripts and Kubernetes manifests are located in the `infra/` and `k8s/` directories.*

1. Clone the repository
2. Set up LocalStack for local AWS service emulation
3. Build and start services using Docker Compose or Kubernetes manifests
4. Access API documentation via the exposed OpenAPI endpoints

---

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

---
