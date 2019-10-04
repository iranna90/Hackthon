from flask import Flask, escape, request
import json
from pprint import pprint

app = Flask(__name__)

@app.route('/input',  methods=['POST'])
def hello():
    incoming_request = request.get_json()
    pprint(incoming_request)
    item_availability = 5
    if not incoming_request.has_key("itemID"):
        return '{"success": "false", "error": "Missing itemID"}'
    if incoming_request["itemID"] == 0:
        item_availability = 0
    response = {
            "item_availability": item_availability,
            "success": True
    }
    return json.dumps(response)
