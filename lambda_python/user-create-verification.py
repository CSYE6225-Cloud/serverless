import email
import json
import os
import boto3

OK = '200 OK'
SUBJET = 'CSYE6225 Webapp Account Register Confirmation'
SEND_NAME = "noreply"
CONFRIM_URI =  "/v1/verifyUserEmail"
EMAIL_TEMP = """
    <p>Hello {},</p>
    <p>You're receiving this email because your email address was used to register a webapp account.</p>
    <p>Please click this link to verify your account register:<br>
    <a href=\"http://{}\">{}</a><br>
    the link will be expired in {} minutes
    </p>Kind Regards,<br>
    CSYE6225 Spring 2022<br>
    {}</p>
"""

def lambda_handler(event, context):
    print("## START: ")
    print('## EVENT: ')
    print(event)
    message = json.loads(event['Records'][0]['Sns']['Message'])
    print(message)

    # check email sent status
    dynamodb = boto3.resource('dynamodb')
    table = dynamodb.Table(os.getenv('EMAIL_TRACKER_TABLE'))
    response = table.get_item(
        Key = {
            'email': message['email']
        }
    )
    print('## ITEM:')
    print(response)
    if 'Item' in response:
        print("## MESSAGE: Already sent email to: " + message['email'])
        return

    # send emails
    ses_client = boto3.client('ses')
    region = os.environ['AWS_REGION']
    domain = os.environ['DOMAIN']
    sender = SEND_NAME + '@' + domain
    verification_link = domain + CONFRIM_URI + '?email=' + message["email"] + "&token=" + message['token']
    content = EMAIL_TEMP.format(message['first_name'], verification_link, verification_link, os.getenv('EXPIRED_TIME'), domain)
    send(ses_client, sender, message['email'], SUBJET, content)
    
    # record in DynamoDB
    table.put_item(
        Item = {
            'email': message['email']
        }
    )

    return OK

def send(ses_client, sender, recipient, subject, bodyHtml):
    response = ses_client.send_email(
        Source = sender,
        Destination = {
            'ToAddresses': [
                recipient
            ],
        },
        Message = {
            'Subject': {
                'Data': subject,
                'Charset': 'UTF-8'
            },
            'Body': {
                'Html': {
                    'Data': bodyHtml,
                    'Charset': 'UTF-8'
                }
            }
        }
    )
    print(response)
