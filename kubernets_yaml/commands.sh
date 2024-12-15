kubectl delete -n default deployment scc2425-webapp postgres redis blob-http-trigger
kubectl delete pvc postgres-pvc redis-pvc media-pvc
kubectl delete -n default secret db-user-secret
kubectl delete -n default service scc2425-webapp-service postgres redis kubernetes blob-http-trigger-service
kubectl delete persistentvolume media-pv postgres-pv redis-pv

kubectl create secret generic db-user-secret \
  --from-literal=DB_USER=admin \
  --from-literal=DB_PASSWORD=AdminPassword \
  --from-literal=POSTGRES_USER=citus \
  --from-literal=POSTGRES_PASSWORD=Admin1234 \
  --from-literal=POSTGRES_URL=jdbc:postgresql://postgres:5432/postgres-scc-2425 \
  --from-literal=SECRET_TOKEN=secret

kubectl apply -f postgres-pvc.yaml
kubectl apply -f postgres.yaml

kubectl apply -f redis-pvc.yaml
kubectl apply -f redis.yaml

kubectl apply -f blob-pvc.yaml
kubectl apply -f blob.yaml

kubectl wait --for=condition=ready pod -l app=postgres

kubectl wait --for=condition=ready pod -l app=redis

kubectl apply -f webapp.yaml

kubectl apply -f blob-http-trigger.yaml

kubectl wait --for=condition=ready pod -l app=scc2425-webapp

#kubectl port-forward svc/scc2425-webapp-service 8080:80
#kubectl rollout restart deployment/scc2425-webapp