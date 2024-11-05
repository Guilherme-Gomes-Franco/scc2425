import azure.functions as func
import logging
import os
from azure.cosmos import CosmosClient, exceptions
import json

app = func.FunctionApp(http_auth_level=func.AuthLevel.FUNCTION)

# Cosmos DB connection settings (set in Application Settings)
COSMOS_DB_ENDPOINT = os.getenv("COSMOS_DB_ENDPOINT")
COSMOS_DB_KEY = os.getenv("COSMOS_DB_KEY")
DATABASE_NAME = "scc2324"  # Replace with your database name
CONTAINER_NAME = "users"  # Replace with your container name

# Initialize the Cosmos DB client
client = CosmosClient(COSMOS_DB_ENDPOINT, COSMOS_DB_KEY)
database = client.get_database_client(DATABASE_NAME)
container = database.get_container_client(CONTAINER_NAME)

@app.route(route="update_views", methods=['POST'])  # Event Grid sends POST requests
def update_views(req: func.HttpRequest) -> func.HttpResponse:
    logging.info("Azure Function received an Event Grid event.")

    try:
        # Parse the JSON data from the Event Grid payload
        event_data = req.get_json()
        
        # Event Grid events may contain multiple events, so we iterate through them
        for event in event_data:
            # Check if the event corresponds to a successful access of the specified URL
            url = event.get("data", {}).get("url")
            status_code = event.get("data", {}).get("statusCode")

            if url and "tukano-scc-61052.azurewebsites.net/rest/blobs/" in url and status_code and status_code.startswith("2"):
                # Extract blob_id from the URL
                blob_id = url.split("/")[-1].split("?")[0]  # Assumes the blob_id is part of the URL path
                
                # Update the item in Cosmos DB
                item = container.read_item(item=blob_id, partition_key=blob_id)
                item['views'] = item.get('views', 0) + 1  # Increment the view count
                
                container.upsert_item(item)
                logging.info(f"Updated item with blob_id {blob_id} in Cosmos DB.")

        return func.HttpResponse("Event processed successfully", status_code=200)

    except exceptions.CosmosResourceNotFoundError:
        logging.error("Item not found in Cosmos DB.")
        return func.HttpResponse("Item not found", status_code=404)

    except Exception as e:
        logging.error(f"An error occurred: {str(e)}")
        return func.HttpResponse("An error occurred while processing the event", status_code=500)