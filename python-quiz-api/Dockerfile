FROM python:3.12-slim

WORKDIR /app

COPY python-quiz-api/requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

COPY python-quiz-api/app ./app
COPY referentials ./referentials

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
