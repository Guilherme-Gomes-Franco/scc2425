kubectl delete -n default deployment scc2425-webapp postgres redis
kubectl delete pvc postgres-pvc redis-pvc media-pvc
kubectl delete -n default configmap db-init-scripts
kubectl delete -n default secret db-user-secret
kubectl delete -n default service scc2425-webapp-service postgres redis kubernetes
kubectl delete persistentvolume media-pv postgres-pv redis-pv

kubectl create secret generic db-user-secret \
  --from-literal=DB_USER=admin \
  --from-literal=DB_PASSWORD=AdminPassword \
  --from-literal=POSTGRES_USER=citus \
  --from-literal=POSTGRES_PASSWORD=Admin1234

chmod +x init_user.sh
kubectl create configmap db-init-scripts --from-file=init_user.sh

kubectl apply -f postgres-pvc.yaml
kubectl apply -f postgres.yaml

kubectl apply -f redis-pvc.yaml
kubectl apply -f redis.yaml

kubectl apply -f blob-pvc.yaml
kubectl apply -f blob.yaml

kubectl wait --for=condition=ready pod -l app=postgres

kubectl wait --for=condition=ready pod -l app=redis

kubectl apply -f webapp.yaml

kubectl wait --for=condition=ready pod -l app=scc2425-webapp

#kubectl port-forward svc/scc2425-webapp-service 8080:80
#kubectl rollout restart deployment/scc2425-webapp