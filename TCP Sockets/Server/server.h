#ifndef SERVER_H
#define SERVER_H

#include <QtWidgets>
#include <QtNetwork>
#include <QNetworkConfigurationManager>
#include <stdlib.h>

class Server : public QDialog
{
    Q_OBJECT

public:
    explicit Server(QWidget *parent = Q_NULLPTR);

private slots:
    void sessionOpened();

private:
    QLabel *statusLabel;
    QTcpServer *tcpServer;
    QNetworkSession* networkSession;
};

#endif // SERVER_H
