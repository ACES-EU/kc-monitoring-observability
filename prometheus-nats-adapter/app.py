from flask import Flask, request
from nats.aio.client import Client as NATS
from nats.js.api import StreamConfig
from prometheus_pb2 import WriteRequest
import os
import snappy
import logging

app = Flask(__name__)


async def send_to_jetstream(nc, subject, data):
    js = nc.jetstream()

    # Define or get the stream
    stream_name = os.getenv("NATS_STREAM", "prometheus")
    await js.add_stream(StreamConfig(name=stream_name, subjects=[subject]))

    # Publish to the subject
    await js.publish(subject, data.encode())



@app.route('/receive', methods=['POST'])
async def receive():
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
        for series in write_request.timeseries:
            logging.info(f"Received timeseries: {series}")

        # Convert the data to a string or appropriate format
        data = str(write_request)

        # Connect to NATS
        nats_url = os.getenv("NATS_URL", "nats://localhost:4222")
        nc = NATS()
        await nc.connect(nats_url)

        # Send the data to NATS JetStream
        nats_subject = os.getenv("NATS_SUBJECT", "metrics")
        await send_to_jetstream(nc, nats_subject, data)

        await nc.close()

        return "Received", 200

    except Exception as e:
        logging.error(f"Failed to process WriteRequest: {e}")
        return "Failed to process WriteRequest", 400


if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)
    app.run(host='0.0.0.0', port=5000)
