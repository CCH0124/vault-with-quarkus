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

驗證 KV 引擎

```bash
$ curl -k https://app.cch.com:8451/info
{"message":"vault hello!"}
```

## Secrets Store CSI Driver 整合

>K3d 版本要 v5.6.0，否則會有[CreateContainerError](https://github.com/kubernetes-sigs/secrets-store-csi-driver/issues/1017)
>運行 K3d 需多做這一步 [K3D_FIX_MOUNTS ](https://github.com/k3d-io/k3d/pull/1268)

[Install using Helm](https://secrets-store-csi-driver.sigs.k8s.io/getting-started/installation)
```bash
helm repo add secrets-store-csi-driver https://kubernetes-sigs.github.io/secrets-store-csi-driver/charts
helm install csi-secrets-store secrets-store-csi-driver/secrets-store-csi-driver --version 1.4.0 --namespace kube-system --set syncSecret.enabled=true
```


安裝完後每個節點都會有一個 POD，該 POD 是由 `DaemonSet` 所產生

```bash
$ kubectl get pods -A -l app.kubernetes.io/instance=csi-secrets-store -o wide
NAMESPACE     NAME                                               READY   STATUS    RESTARTS   AGE     IP          NODE                           NOMINATED NODE   READINESS GATES
kube-system   csi-secrets-store-secrets-store-csi-driver-ss22c   3/3     Running   0          3h38m   10.42.1.4   k3d-quarkus-cluster-server-0   <none>           <none>
kube-system   csi-secrets-store-secrets-store-csi-driver-vg547   3/3     Running   0          3h38m   10.42.0.4   k3d-quarkus-cluster-agent-0    <none>           <none>
```

安裝 [vault-csi-provider](https://github.com/hashicorp/vault-csi-provider)，以取得儲存在 Vault 中的資訊，並使用 Secrets Store CSI 介面將它們掛載到 Kubernetes POD 中。

```bash
helm install vault hashicorp/vault --version 0.26.1 --namespace vault --create-namespace  --set "server.enabled=false" --set "injector.enabled=false" --set "csi.enabled=true"
```

如果不安裝，則會導致 Secrets Store CSI 介面不知道如何整合 Vault
```bash
  Warning  FailedMount  22m (x13 over 32m)    kubelet            MountVolume.SetUp failed for volume "vault-secret-env" : rpc error: code = Unknown desc = failed to mount secrets store objects for pod default/secret-csi-vault-7b98fd57d5-5kxfw, err: error connecting to provider "vault": provider not found: provider "vault"
  Warning  FailedMount  12m (x8 over 30m)     kubelet            Unable to attach or mount volumes: unmounted volumes=[vault-secret-env vault-secret-file], unattached volumes=[], failed to process volumes=[]: timed out waiting for the condition
```
該 Secret store CSI 能夠整合第三方服務可參考官方的[資訊](https://secrets-store-csi-driver.sigs.k8s.io/providers#features-supported-by-current-providers)。本實驗會整合 Vault。

首先礙於 K3d 環境，這邊 Vault 使用 NodePort 方式將 Vault 服務給導出。

```bash
$ kubectl get svc -n vault
NAME                       TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)                         AGE
...
vault                      NodePort    10.43.27.225    <none>        8200:30820/TCP,8201:31082/TCP   18h
# 找出 vault 所屬的節點
$ kubectl get pods -o wide -n vault
NAME                                  READY   STATUS    RESTARTS   AGE   IP          NODE                         NOMINATED NODE   READINESS GATES
vault-agent-injector-7fc4b89f-8hjk9   1/1     Running   0          18h   10.42.1.5   k3d-vault-cluster-agent-1    <none>           <none>
vault-0                               1/1     Running   0          18h   10.42.2.8   k3d-vault-cluster-server-0   <none>           <none>
# 獲取 Vault 所屬節點的 IP 
$ kubectl get node -o wide
NAME                         STATUS   ROLES                  AGE   VERSION        INTERNAL-IP   EXTERNAL-IP   OS-IMAGE   KERNEL-VERSION                       CONTAINER-RUNTIME
k3d-vault-cluster-server-0   Ready    control-plane,master   18h   v1.27.7+k3s1   172.20.0.5    <none>        K3s dev    5.15.133.1-microsoft-standard-WSL2   containerd://1.7.7-k3s1.27
k3d-vault-cluster-agent-1    Ready    <none>                 18h   v1.27.7+k3s1   172.20.0.7    <none>        K3s dev    5.15.133.1-microsoft-standard-WSL2   containerd://1.7.7-k3s1.27
k3d-vault-cluster-agent-0    Ready    <none>                 18h   v1.27.7+k3s1   172.20.0.6    <none>        K3s dev    5.15.133.1-microsoft-standard-WSL2   containerd://1.7.7-k3s1.27
```

準備好上述的鍵值對後，透過 Secret Store CSI 提供的 CRD 進行 Vault 存取宣告，這邊會分成基於 `ENV` 和 `FILE` 來做分享。

**使用 ENV 方式宣告**

建立一個可透過 Secret Store CSI 提供的 CRD 來存取 Vault 中 KV 資源。預設上，KV 資源丟什麼值就取什麼值。

```yaml
# ENV base
apiVersion: secrets-store.csi.x-k8s.io/v1
kind: SecretProviderClass
metadata:
  name: quarkus-demo-env
  labels:
    app.kubernetes.io/managed-by: "vault"
spec:
  provider: vault
  parameters:
    roleName: 'quarkus-vault'
    vaultAddress: 'http://172.20.0.5:30820'
    vaultAuthMountPath: 'quarkus-cluster'
    objects: |
      - objectName: "greeting.message"
        secretPath: "kv/data/quarkus/vault-demo?version=1"
        secretKey: "greeting.message"
  secretObjects:
    - data:
      - key: greeting.message
        objectName: greeting.message
      secretName: quarkus
      type: Opaque
      labels:
        app.kubernetes.io/managed-by: "quarkus"
```

當部署 `kubectl apply -f k8s/deployment-secret-csi.yaml` 後可以發現，在上述使用 ENV 方式建立的 `SecretProviderClass` 物件，透過 `secretObjects` 欄位建立一個 `secret` 物件，如下

```bash
$ kubectl get secret
NAME         TYPE                                  DATA   AGE
...
quarkus      Opaque                                1      51m
```

其 `secret` 資源可以被引用，在 `deployment` 物件中可宣告如下引用。

```yaml
...
      containers:
        - env:
            - name: KUBERNETES_NAMESPACE
              valueFrom:
                fieldRef:
                  fieldPath: metadata.namespace
            - name: GREETING_MESSAGE # 這邊喔
              valueFrom:
                secretKeyRef:
                  name: quarkus
                  key: greeting.message
...
          volumeMounts: # 這邊喔
...
          - name: vault-secret-env
            mountPath: "/mnt/secrets-store"
            readOnly: true
...
      volumes: # 這邊喔
...
      - name: vault-secret-env
        csi:
          driver: 'secrets-store.csi.k8s.io'
          readOnly: true
          volumeAttributes:
            secretProviderClass: 'quarkus-demo-env'
...
```

透過此範例的 API 進行驗證

```bash
$ curl -k https://app.cch.com:8451/info
{"message":"vault hello!"}
```

>這邊要注意的是如果要啟用 `secretObjects` 功能，即讓 CSI 介面幫你建立一個 `secret` 物件，要在安裝 CSI 時啟用該功能 `--set syncSecret.enabled=true`
>

**使用 FILE 方式宣告**

在上面步驟建立的 `quarkus/vault-demo` 路徑下新增一個 `fullchain.crt` 的鍵值對，本範例需要一個憑證掛載至服務中才能執行驗證終端憑證的 API。這邊會將 pki 目錄下的檔案進行 base64 編碼在寫入 Vault 中 KV 資源。

```bash
$ secret-csi-vault/pki$ export FULL_CHAIN=$(cat ca-bundle.crt | base64 --wrap=1000)
$ vault kv put kv/quarkus/vault-demo fullchain.crt="$FULL_CHAIN"
======= Secret Path =======
kv/data/quarkus/vault-demo

======= Metadata =======
Key                Value
---                -----
created_time       2023-12-10T04:28:11.532295585Z
custom_metadata    <nil>
deletion_time      n/a
destroyed          false
version            3
```

相較於 ENV 方式，FILE 方式不必宣告 `secretObjects`。表示其用檔案方式進行掛載，這邊可以看到的是 `encoding` 欄位使用 `base64`，表示要將 KV 中 `fullchain.crt` 鍵對應的值進行 `base64` 解碼。

```yaml
# File based
apiVersion: secrets-store.csi.x-k8s.io/v1
kind: SecretProviderClass
metadata:
  name: quarkus-demo-file
  labels:
    app.kubernetes.io/managed-by: "vault"
spec:
  provider: vault
  parameters:
    roleName: 'quarkus-vault'
    vaultAddress: 'http://172.20.0.5:30820'
    vaultAuthMountPath: 'quarkus-cluster'
    objects: |
      - objectName: "fullchain.crt"
        secretPath: "kv/data/quarkus/vault-demo?version=3"
        secretKey: "fullchain.crt"
        encoding: "base64"
```

同樣，在 `Deployment` 資源的宣告下要將上面的 `SecretProviderClass` 物件進行掛載，如下。這如同 ENV 方式，但因為是 FILE 形式所以不必宣告 `env` 欄位。

```yaml
...
          volumeMounts:
...
          - name: vault-secret-file
            mountPath: "/opt/pki"
            readOnly: true
      volumes:
...
      - name: vault-secret-file
        csi:
          driver: 'secrets-store.csi.k8s.io'
          readOnly: true
          volumeAttributes:
            secretProviderClass: 'quarkus-demo-file'

```

在 `mountPath` 欄位定義了要將 Vault 取出的資源放置 `/opt/pki` 路徑下，下述透過指令驗證

```bash
$ kubectl exec secret-csi-vault-7b98fd57d5-cjddz -- ls /opt/pki
fullchain.crt
```

進行 API 驗證，並回應 `200`。

```bash
$ curl -v -k -XPOST --form client=@end-entity.crt https://app.cch.com:8451
...
< HTTP/2 200
< date: Sun, 10 Dec 2023 07:24:50 GMT
< content-length: 0
< strict-transport-security: max-age=15724800; includeSubDomains
<
* Connection #0 to host app.cch.com left intact
```


上面 ENV 或 FILE 簡易的流程是 `kubelet` 在 POD `volume` 掛載期間調用 CSI 驅動程式。因此，在 POD 啟動後，後續更改不會觸發對該掛載或 Kubernetes 金鑰中內容的更新。如果其配置有像是找不到 Vault 中定義的 Key 問題則 POD 會處於 `ContainerCreating` 狀態，如下

```bash
$ kubectl get pods -w
secret-csi-vault-7b98fd57d5-gmvw6   0/1     ContainerCreating   0          24s
$ kubectl describe pods secret-csi-vault-7b98fd57d5-gmvw6
...
  Normal   Scheduled    116s                default-scheduler  Successfully assigned default/secret-csi-vault-7b98fd57d5-gmvw6 to k3d-quarkus-cluster-agent-0
  Warning  FailedMount  52s (x8 over 116s)  kubelet            MountVolume.SetUp failed for volume "vault-secret-file" : rpc error: code = Unknown desc = failed to mount secrets store objects for pod default/secret-csi-vault-7b98fd57d5-gmvw6, err: rpc error: code = Unknown desc = error making mount request: {kv/data/quarkus/vault-demo}: {key "fullchain.crt" does not exist at the secret path}
```

下圖為官方提供的架構圖

![](https://secrets-store-csi-driver.sigs.k8s.io/images/diagram.png)

在 POD 啟動或重新啟動時，Secrets Store CSI 使用 gRPC 與提供者通訊以上面範例是使用 `vault`，從 `SecretProviderClass` 資源中指定的外部檢索內容。然後該磁碟區作為 `tmpfs` 掛載到 POD 中，並將檢所內容寫入該磁碟區。將 POD 刪除時，*相應的資源將被清理並刪除*。更多的細節可至官方進行[翻閱](https://secrets-store-csi-driver.sigs.k8s.io/concepts)。


本文章範例透過 `SecretProviderClass` CRD 進行定義，其提供了介面給第三方界接。透過 `kubectl explain` 來看，其提供 `parameters` 給第三方進行串接。

```bash
$ kubectl explain SecretProviderClass.spec
GROUP:      secrets-store.csi.x-k8s.io
KIND:       SecretProviderClass
VERSION:    v1

FIELD: spec <Object>

DESCRIPTION:
    SecretProviderClassSpec defines the desired state of SecretProviderClass

FIELDS:
  parameters    <map[string]string>
    Configuration for specific provider

  provider      <string>
    Configuration for provider name

  secretObjects <[]Object>
    <no description>
```

實際上本文章範例定義的 `parameters`，是透過 Vault 官方的規範進行配置，下面 `parameters` 下描述的欄位都參考至 Vault 針對於 CSI 的[配置](https://developer.hashicorp.com/vault/docs/platform/k8s/csi/configurations)。這邊進行簡易的配置描述

- roleName 登入 Vault 時要使用的角色名稱
- vaultAddress Vault 服務通訊位置
- vaultAuthMountPath Vault 中定義 auth 的路徑，用於登入時呼叫登入 API 路徑
- objects 檢索 Vault 資源

```yaml
...
  parameters:
    roleName: 'quarkus-vault'
    vaultAddress: 'http://172.20.0.5:30820'
    vaultAuthMountPath: 'quarkus-cluster'
    objects: |
      - objectName: "fullchain.crt"
        secretPath: "kv/data/quarkus/vault-demo?version=3"
        secretKey: "fullchain.crt"
        encoding: "base64"
```
