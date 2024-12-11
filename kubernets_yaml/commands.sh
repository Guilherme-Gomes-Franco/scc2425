kubectl create secret generic db-user-secret --from-literal=DB_ADMIN_USER=admin --from-literal=DB_ADMIN_PASSWORD=secretpassword
kubectl create configmap db-init-scripts --from-file=init_user.sh
kubectl apply -f postgres-pvc.yaml
kubectl apply -f postgres.yaml
#kubectl port-forward svc/postgres 5432:5432
kubectl apply -f redis-pvc.yaml
kubectl apply -f redis.yaml
#kubectl port-forward svc/redis 6379:6379
kubectl apply -f blob-pvc.yaml
kubectl apply -f blob.yaml
kubectl apply -f webapp.yaml
kubectl port-forward svc/scc2425-webapp-service 8080:80
#kubectl rollout restart deployment/scc2425-webapp