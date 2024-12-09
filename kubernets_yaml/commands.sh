kubectl apply -f postgres-pvc.yaml
kubectl apply -f postgres.yaml
#kubectl port-forward svc/postgres 5432:5432
kubectl apply -f redis-pvc.yaml
kubectl apply -f redis.yaml
#kubectl port-forward svc/redis 6379:6379
kubectl apply -f blob-pvc.yaml
kubectl apply -f blob.yaml