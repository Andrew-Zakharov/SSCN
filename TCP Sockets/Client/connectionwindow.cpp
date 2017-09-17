#include "connectionwindow.h"
#include "ui_connectionwindow.h"

ConnectionWindow::ConnectionWindow(QWidget *parent) :
    QMainWindow(parent),
    ui(new Ui::ConnectionWindow),
    mainWindow(Q_NULLPTR)
{
    ui->setupUi(this);
    this->setFixedSize(this->size());
}

void ConnectionWindow::onConnectClicked()
{
    this->hide();
    mainWindow = new MainWindow(ui->serverNameEdit->text(), ui->serverPortEdit->text().toInt());
    mainWindow->show();
}

void ConnectionWindow::onServerSettingsChanged()
{
    QString serverName = ui->serverNameEdit->text();
    QString serverPort = ui->serverPortEdit->text();
    bool wrongSettings = false;

    if(!serverName.isEmpty() && !serverPort.isEmpty())
    {
        QStringList serverAddressOctets = serverName.split(".");
        if(serverAddressOctets.size() == 4)
        {
            for(qint64 i = 0; i < serverAddressOctets.size(); i++)
            {
                if((serverAddressOctets.at(i)).toInt() < 0 ||
                    serverAddressOctets.at(i).toInt() > 255)
                {
                    wrongSettings = true;
                    break;
                }
            }
        }
        else
        {
            wrongSettings = true;
        }

        if(serverPort.toInt() < 0 || serverPort.toInt() > 65535)
        {
            wrongSettings = true;
        }
    }
    else
    {
        wrongSettings = true;
    }

    ui->connectButton->setEnabled(!wrongSettings);
}

ConnectionWindow::~ConnectionWindow()
{
    delete ui;
}
