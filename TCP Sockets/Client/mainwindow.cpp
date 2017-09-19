#include "mainwindow.h"
#include "ui_mainwindow.h"

MainWindow::MainWindow(const QString& hostName, quint16 port, QWidget *parent) :
    QMainWindow(parent),
    tcpSocket(new QTcpSocket(this)),
    ui(new Ui::MainWindow)
{
    ui->setupUi(this);

    connect(tcpSocket, SIGNAL(connected()), this, SLOT(socketConnected()));
    connect(tcpSocket, SIGNAL(readyRead()), this, SLOT(socketReadyRead()));
    connect(tcpSocket, SIGNAL(error(QAbstractSocket::SocketError)), this, SLOT(socketError(QAbstractSocket::SocketError)));

    tcpSocket->connectToHost(hostName, port);
}

void MainWindow::OnSendClicked()
{
    if(!ui->messageInput->text().isEmpty())
    {
        QByteArray block;
        QDataStream out(&block, QIODevice::WriteOnly);
        out.setVersion(QDataStream::Qt_5_9);
        out << ui->messageInput->text() << "\n";
        tcpSocket->write(block);

        ui->messageInput->clear();
    }
}

void MainWindow::socketConnected()
{
    ui->logTextEdit->append(tr("Connected to server\n"));
}

void MainWindow::socketReadyRead()
{

}

void MainWindow::socketError(QAbstractSocket::SocketError socketError)
{
    ui->logTextEdit->append(tr("Error occured\n"));
}

MainWindow::~MainWindow()
{
    delete ui;
}
