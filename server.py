import os
from http.server import BaseHTTPRequestHandler, HTTPServer
import json
from openai import OpenAI

client = OpenAI(api_key=os.environ["OPENAI_API_KEY"])

class Handler(BaseHTTPRequestHandler):

    def do_POST(self):
        if self.path != "/chat":
            self.send_response(404)
            self.end_headers()
            return

        length = int(self.headers.get("Content-Length", 0))
        data = json.loads(self.rfile.read(length))

        message = data.get("message", "").strip()

        if not message:
            self.send_response(400)
            self.end_headers()
            return

        response = client.responses.create(
            model="gpt-5.6-luna",
            input=message
        )

        result = {
            "reply": response.output_text
        }

        body = json.dumps(result).encode("utf-8")

        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


server = HTTPServer(("0.0.0.0", 8080), Handler)

print("JARVIS AI SERVER IS RUNNING")

server.serve_forever()
