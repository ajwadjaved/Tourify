import sys
from flask import Flask, request, jsonify
import traceback
import pandas as pd
import joblib
from zipfile import ZipFile

# Import the main_model and models dictionary from models.py
from models import models, main_model

# Your API definition
app = Flask(__name__)

@app.route('/predict', methods=['POST'])
def predict():
    try:
        json_ = request.json
        # print(json_)

        # Extract the input parameters from the JSON data
        try:
            taxi_locationID = json_['taxi_locationID']
        except KeyError:
            taxi_locationID = None
            
        try:
            name_alias = json_['name_alias']
        except KeyError:
            name_alias = None
            
        try:
            passengers = json_['passengers']
        except KeyError:
            passengers = None     
            
        month = json_['month']
        day_of_week = json_['day_of_week']
        hour = json_['hour']
        temp_avg = json_['temp_avg']
        precipitation = json_['precipitation']

        # Check if the taxi_locationID exists in the models dictionary
        if taxi_locationID in models:
            # Load the corresponding model based on the taxi_locationID from the zip archive
            with ZipFile('pickle_files/backup_pickle_files.zip', 'r') as archive:
                model_filename = f'{taxi_locationID}.pkl'
                with archive.open(model_filename) as file:
                    model = joblib.load(file)

            # Create a new DataFrame with the input data for prediction
            new_data = pd.DataFrame({
                'month': [month],
                'day_of_week': [day_of_week],
                'hour': [hour],
                'taxi_locationID': [taxi_locationID],
                'passengers': [passengers],
                'temp_avg': [temp_avg],
                'precipitation': [precipitation]
            })

            # Use the loaded model to make predictions
            predictions = model.predict(new_data)

        else:
            # Create a new DataFrame with the input data for prediction
            new_data = pd.DataFrame({
                'month': [month],
                'day_of_week': [day_of_week],
                'hour': [hour],
                'name_alias': [name_alias], 
                'temp_avg': [temp_avg],
                'precipitation': [precipitation]
            })

            # Use the main_model to make predictions
            predictions = main_model.predict(new_data)

        # Round the prediction to the nearest whole number
        prediction = round(predictions[0])

        # If prediction is greater than 100, set it to 100
        if prediction > 100:
            prediction = 100

        return jsonify({'prediction': prediction})

    except:
        return jsonify({'trace': traceback.format_exc()})

if __name__ == '__main__':
    try:
        port = int(sys.argv[1])  # This is for a command-line input
    except:
        port = 12345  # If you don't provide any port, the port will be set to 12345

    # print('Models loaded')
    app.run(port=port, debug=True)