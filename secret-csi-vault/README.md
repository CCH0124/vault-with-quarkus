# secret-csi-vault

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./gradlew quarkusDev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
./gradlew build
```
It produces the `quarkus-run.jar` file in the `build/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `build/quarkus-app/lib/` directory.

The application is now runnable using `java -jar build/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./gradlew build -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar build/*-runner.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
./gradlew build -Dquarkus.package.type=native
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./gradlew build -Dquarkus.package.type=native -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./build/secret-csi-vault-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/gradle-tooling.

## Related Guides

- RESTEasy Reactive ([guide](https://quarkus.io/guides/resteasy-reactive)): A Jakarta REST implementation utilizing build time processing and Vert.x. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it.
- Vault ([guide](https://quarkiverse.github.io/quarkiverse-docs/quarkus-vault/dev/index.html)): Store your credentials securely in HashiCorp Vault

## Provided Code

### RESTEasy Reactive

Easily start your Reactive RESTful Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)


## Create K3d 

```bash
cd k3d
k3d$ k3d cluster create -c config.yaml --servers-memory 2GB --agents-memory 2GB
```

## Create K/V engine on vault
1. 配置登入需求
```bash
export VAULT_ADDR=https://vault-demo.cch.com:8453
export VAULT_SKIP_VERIFY=true
vault login -tls-skip-verify hvs.2fVKDYNbdS1kxa186pWZuDWn
```

2. 啟用 K/V 後端

```bash
$ vault secrets enable -version=2 kv
Success! Enabled the kv secrets engine at: kv/
```

3. 定義 greeting.message 的鍵值

```bash
$ vault kv put kv/quarkus/vault-demo greeting.message="vault hello!"
======= Secret Path =======
kv/data/quarkus/vault-demo

======= Metadata =======
Key                Value
---                -----
created_time       2023-12-03T04:48:18.833082542Z
custom_metadata    <nil>
deletion_time      n/a
destroyed          false
version            1
```
## access role

```bash
# policy.hcl
path "kv/data/quarkus/vault-demo" {
    capabilities = ["read"]
}
vault policy write quarkus policy.hcl
```


## Integrate Multiple Kubernetes Clusters to Vault Server

建置給 Vault 做 Kubernetes token 認證的配置，其中會使用以下 `Secret` 物件生成的長期 token。
```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: vault-auth
  labels:
    app.kubernetes.io/managed-by: vault
---
apiVersion: v1
kind: Secret
metadata:
  name: vault-auth
  annotations:
    kubernetes.io/service-account.name: vault-auth
type: kubernetes.io/service-account-token
---
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
   name: role-tokenreview-binding
roleRef:
   apiGroup: rbac.authorization.k8s.io
   kind: ClusterRole
   name: system:auth-delegator
subjects:
- kind: ServiceAccount
  name: vault-auth
  namespace: default
```

配置並建立一個基於 kubernetes 的 auth
```bash
export SA_JWT_TOKEN=$(kubectl get secret  vault-auth -o jsonpath="{ .data.token }" | base64 --decode; echo)
kubectl -n default get secret  vault-auth -o jsonpath="{.data['ca\.crt']}" | base64 --decode | tee -a ca.crt
export SA_CA_CRT=$(cat ca.crt)
kubectl config view --raw --minify --flatten -o jsonpath='{.clusters[].cluster.server}'
```

```bash
$ vault auth enable --path=quarkus-cluster kubernetes
Success! Enabled kubernetes auth method at: quarkus-clust
```

```bash
$ vault write auth/quarkus-cluster/config token_reviewer_jwt=$SA_JWT_TOKEN kubernetes_host=https://172.20.0.6:6443 kubernetes_ca_cert="$(cat ca.crt)"
$ vault read  auth/quarkus-cluster/config
Key                       Value
---                       -----
disable_iss_validation    true
disable_local_ca_jwt      false
issuer                    n/a
kubernetes_ca_cert        -----BEGIN CERTIFICATE-----
MIIBdzCCAR2gAwIBAgIBADAKBggqhkjOPQQDAjAjMSEwHwYDVQQDDBhrM3Mtc2Vy
dmVyLWNhQDE3MDE1OTUyNTQwHhcNMjMxMjAzMDkyMDU0WhcNMzMxMTMwMDkyMDU0
WjAjMSEwHwYDVQQDDBhrM3Mtc2VydmVyLWNhQDE3MDE1OTUyNTQwWTATBgcqhkjO
PQIBBggqhkjOPQMBBwNCAAROecAvJxanXKn5xhGgPAIr4vCSkUON/gTK804MxLAw
AaYiNJIHZcBCsF2HrLjno9Z5WXgMr4auihygOCH+duMEo0IwQDAOBgNVHQ8BAf8E
BAMCAqQwDwYDVR0TAQH/BAUwAwEB/zAdBgNVHQ4EFgQUIZoFjtr+3eM5JOD6Bj7r
CtjOPjUwCgYIKoZIzj0EAwIDSAAwRQIgPASRbtat2BdrxFfhN2vE4mJqt+G4Q3c6
WPp0RVUPPYACIQDBNsAVzwTTCBYTd49DrVd3KyA6q19nY21bxMtVMHjxtw==
-----END CERTIFICATE-----
kubernetes_host           https://172.20.0.6:6443
pem_keys                  []
```

`kubernetes_host` 使用 k3d 建置出來的 lb 容器 IP 位置。

建立 auth 中的 role
```bash
vault write auth/quarkus-cluster/role/quarkus-vault \
    bound_service_account_names=* \
    bound_service_account_namespaces=* \
    policies=quarkus \
    ttl=24h
```


[官方文檔](https://developer.hashicorp.com/vault/docs/auth/kubernetes)


## Secret CSI

>K3d 版本要 v5.6.0，否則會有[CreateContainerError](https://github.com/kubernetes-sigs/secrets-store-csi-driver/issues/1017)
>運行 K3d 需多做這一步 [K3D_FIX_MOUNTS ](https://github.com/k3d-io/k3d/pull/1268)

[Install using Helm](https://secrets-store-csi-driver.sigs.k8s.io/getting-started/installation)
```bash
helm repo add secrets-store-csi-driver https://kubernetes-sigs.github.io/secrets-store-csi-driver/charts
helm install csi-secrets-store secrets-store-csi-driver/secrets-store-csi-driver --version 1.4.0 --namespace kube-system
```
