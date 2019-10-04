FROM python:2.7
WORKDIR /code
ADD main.py /code
ADD requirements.txt /code
RUN pip install -r requirements.txt
ENV FLASK_APP main.py
CMD flask run
