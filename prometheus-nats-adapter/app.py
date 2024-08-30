from flask import Flask, request
from nats.aio.client import Client as NATS
from nats.js.api import StreamConfig
from nats.js.errors import NotFoundError
from prometheus_pb2 import WriteRequest
import os
import snappy
import logging

app = Flask(__name__)

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger('werkzeug')
logger.setLevel(logging.INFO)


async def ensure_stream_exists(js, stream_name, subject):
    logging.info(f"Ensuring stream '{stream_name}' exists.")
    try:
        # Try to get the existing stream configuration
        await js.stream_info(stream_name)
        logging.info(f"Stream '{stream_name}' already exists.")
    except NotFoundError:
        # If the stream does not exist, create it
        logging.info(f"Creating stream '{stream_name}'.")
        await js.add_stream(StreamConfig(name=stream_name, subjects=[subject]))

async def send_to_jetstream(nc, subject, data):

    logging.info("Connecting to JetStream.")

    js = nc.jetstream()

    # Ensure the stream exists
    stream_name = os.getenv("NATS_STREAM", "prometheus")
    await ensure_stream_exists(js, stream_name, subject)

    logging.info(f"Publishing to subject '{subject}' in stream '{stream_name}'.")

    # Publish to the subject
    await js.publish(subject, data.encode(),stream=stream_name)

    logging.info("Published to JetStream.")



@app.route('/receive', methods=['POST'])
async def receive():
    logging.info("Received request.")
    try:
        # Check if the data is compressed
        if request.headers.get("Content-Encoding") == "snappy":
            raw_data = snappy.uncompress(request.data)
        else:
            raw_data = request.data

        # Parse the Prometheus remote write data
        write_request = WriteRequest()
        write_request.ParseFromString(raw_data)

        # Process the WriteRequest (for demonstration, logging the timeseries)
        logging.info(f"Received timeseries: {write_request.timeseries[0]}")

        # Convert the data to a string or appropriate format
        data = str(write_request)

        # Connect to NATS
        logging.info("Connecting to NATS.")
        nats_url = os.getenv("NATS_URL", "nats://localhost:4222")
        nc = NATS()
        await nc.connect(nats_url)

        logging.info("Connected to NATS.")

        # Send the data to NATS JetStream
        nats_subject = os.getenv("NATS_SUBJECT", "metrics")
        await send_to_jetstream(nc, nats_subject, data)

        await nc.close()

        return "Received", 200

    except Exception as e:
        logging.error(f"Failed to process WriteRequest: {e}")
        return "Failed to process WriteRequest", 400


if __name__ == '__main__':
    # set logging level
    logging.basicConfig(level=logging.INFO)
    logging.info("Starting server.")
    app.run(host='0.0.0.0', port=5000, debug=True)
